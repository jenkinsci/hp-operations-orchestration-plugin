// (c) Copyright 2013 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.hp.mercury.ci.jenkins.plugins.oo.core;

import com.google.gson.JsonArray;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.StringUtils;
import com.hp.mercury.ci.jenkins.plugins.oo.http.JaxbEntity;
import com.hp.mercury.ci.jenkins.plugins.OOBuildStep;
import hudson.model.BuildListener;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class OOAccessibilityLayer {

    private static final String REST_SERVICES_URL_PATH = "services/rest/";
    private static final String LIST_OPERATION_URL_PATH = "list/";
    private static final String RUN_OPERATION_URL_PATH = "run/";
    private static final String EXECUTIONS_API = "/executions";

    private static final DefaultHttpClient client = OOBuildStep.getHttpClient();
    private static final JsonParser parser = new JsonParser();

    public static String feedURL;
    private static String jsonExecutionResult;

    public static OOServer getOOServer(String uniqueLabel) {

        return OOBuildStep.getDescriptorStatically().getOoServers(true).get(uniqueLabel);
    }

    public static Collection<String> getAvailableServers() {

        return OOBuildStep.getDescriptorStatically().getOoServers(true).keySet();
    }

    public static OOListResponse listFlows(OOServer s, String... folders) throws IOException {

        String foldersPath = "";

        for (String folder : folders) {
            foldersPath += folder + "/";
        }

        String url = StringUtils.slashify(s.getUrl()) +
                REST_SERVICES_URL_PATH +
                LIST_OPERATION_URL_PATH +
                foldersPath;

        final HttpResponse response = OOBuildStep.getHttpClient().execute(
                new HttpGet(OOBuildStep.URI(url)));

        final int statusCode = response.getStatusLine().getStatusCode();

        final HttpEntity entity = response.getEntity();

        try {

            if (statusCode == HttpStatus.SC_OK) {

                return JAXB.unmarshal(entity.getContent(), OOListResponse.class);
            } else {

                throw new RuntimeException("unable to get list of flows from " + url + ", response code: " +
                        statusCode + "(" + HttpStatus.getStatusText(statusCode) + ")");
            }
        } finally {
            EntityUtils.consume(entity);
        }
    }

    public static OORunResponse run(OORunRequest request) throws IOException, URISyntaxException {

        String urlString = StringUtils.slashify(request.getServer().getUrl()) +
                REST_SERVICES_URL_PATH +
                RUN_OPERATION_URL_PATH +
                StringUtils.unslashifyPrefix(request.getFlow().getId());

        final URI uri = OOBuildStep.URI(urlString);
        final HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(new JaxbEntity(request));
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "text/xml");

        HttpResponse response;

        response = client.execute(httpPost);
        final int statusCode = response.getStatusLine().getStatusCode();
        final HttpEntity entity = response.getEntity();

        try {
            if (statusCode == HttpStatus.SC_OK) {

                return JAXB.unmarshal(entity.getContent(), OORunResponse.class);

            } else {

                throw new RuntimeException("unable to get run result from " + uri + ", response code: " +
                        statusCode + "(" + HttpStatus.getStatusText(statusCode) + ")");
            }
        } finally {
            EntityUtils.consume(entity);
        }
    }

    public static int cancel10x(String urlString) throws IOException, InterruptedException {

        final URI uri = OOBuildStep.URI(urlString + EXECUTIONS_API + "/status");
        final HttpPut httpPut = new HttpPut(uri);
        String body = "{\"action\":\"cancel\"}";
        StringEntity entity = new StringEntity(body);
        httpPut.setEntity(entity);
        httpPut.setHeader("Content-Type", "application/json");   // this is mandatory in order for the request to work
        if (OOBuildStep.getEncodedCredentials() != null) {
            httpPut.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }
        HttpResponse response = client.execute(httpPut);
        return response.getStatusLine().getStatusCode();
    }

    public static String run10x(String selectedFlowUUID, List<OOArg> argsToUse, String urlString, String timeout, BuildListener listener) throws IOException, InterruptedException {

        final URI uri = OOBuildStep.URI(urlString + EXECUTIONS_API);
        final HttpPost httpPost = new HttpPost(uri);

        // form inputs for POST request
        String inputs = "{";
        for (int i = 0; i < argsToUse.size(); i++) {

            inputs += "\"" + argsToUse.get(i).getName() + "\" : \"" + argsToUse.get(i).getValue() + "\"";

            if (i < argsToUse.size() - 1)
                inputs += ",";
        }
        inputs = inputs + "}";

        // Create the POST object and add the parameters

        StringEntity entity = new StringEntity("{\"uuid\" : \"" + selectedFlowUUID + "\", \"inputs\" :" + inputs + "}", "UTF-8");


        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");   // this is mandatory in order for the request to work

        HttpResponse response = client.execute(httpPost);

        HttpEntity responseEntity = response.getEntity();
        InputStream is = responseEntity.getContent();

        JsonObject element = (JsonObject) (parser.parse(OOAccessibilityUtils.convertStreamToString(is)));
        feedURL = element.get("feedUrl").getAsString();

        String executionResult = "";
        String currentExecutionResult = "";

        boolean isCompleted = false;
        int waitCount = 0;
        // wait until the execution on Central completes
        HttpGet httpGet = new HttpGet(feedURL + "?pageSize=10000"); // hack !!!
        httpGet.setHeader("Content-Type", "application/json");
        if (OOBuildStep.getEncodedCredentials() != null) {
            httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        Long timeoutLong;
        int sleepTimes = 10;

        try {
            timeoutLong = Long.parseLong(timeout);
            sleepTimes = (int) (timeoutLong / 60000);
            if (sleepTimes == 0) {
                sleepTimes = 1;    // force sleepTimes to be at least 1
            }

        } catch (NumberFormatException e) {
            timeoutLong = 60000L;
        }
        listener.getLogger().println("Step Execution Timeout : " + timeoutLong + " ms");

        URL url = new URL(urlString.replace("oo/rest/v1",""));

        while ((waitCount < sleepTimes) && (!isCompleted)) {

            if (executionResult.equals(currentExecutionResult)) {       // if nothing changes in the JSON result, the counter starts, else it resets

                if (currentExecutionResult.length() > 0) {              // only count after the first iteration.
                    waitCount++;
                    Thread.sleep(60000L);                               // full sleep of 1 minute
                } else {
                    Thread.sleep(10000L);                               // smaller sleep in the first iterations, until the log is first populated
                }

            } else {
                //get JSON change and print to console
                // listener.annotate(new SimpleHtmlNote("insideAccess"+formatResult(executionResult.replace(currentExecutionResult, ""))));
                waitCount = 0;
                Thread.sleep(10000L);                                   // "happy" sleep of 10 seconds
                currentExecutionResult = executionResult;
            }

            // get feedURL
            executionResult = OOAccessibilityUtils.getStringFromResponse(httpGet);

            if (executionResult.length() > 2) {
                isCompleted = OOAccessibilityUtils.isExecutionComplete(executionResult); // get the status of the execution from the partial JSON returned from the feedURL.
            }
        }

        setJSONExecutionResult(executionResult);
        executionResult = formatResult(executionResult);

        if (waitCount == sleepTimes) {

            String feed = feedURL.replace("/steps", "").replace("executions/", "executions?runId=");
            httpGet = new HttpGet(feed);
            String feed2 = OOAccessibilityUtils.getStringFromResponse(httpGet);

            String status = OOAccessibilityUtils.getExecutionStatus(feed2);

            if (status.equals("COMPLETED")) {
                executionResult += "\nThe execution ended with success after reaching the timeout of " + timeout + " milliseconds \n\n";
            } else {
                return "\n <font color=\"red\"><b>The execution is not complete and exceeded the timeout. It may have been paused or canceled </b></font> \n\n";
            }
        }

        return executionResult;
    }

    public static String formatResult(String executionResult) {

        boolean runIsCanceled = false;
        JsonArray array = (JsonArray) (parser.parse(executionResult));
        JsonObject object;
        StringBuilder resultBuilder = new StringBuilder("");

        for (int i = 0; i < array.size(); i++) {

            object = (JsonObject) array.get(i);
            JsonObject stepInfo = (JsonObject) object.get("stepInfo");
            Date date = new Date();
            try {
                long epoch = Long.parseLong(stepInfo.get("endTime").toString());
                date = new Date(epoch);
            } catch (NumberFormatException e) {
                runIsCanceled = true;
            }

            DateFormat formatter = new SimpleDateFormat("hh:mm:ss");

            String color = "blue";

            if ((stepInfo.get("transitionMessage").toString().toLowerCase().contains("failure")) ||
                    (stepInfo.get("responseType").toString().toLowerCase().contains(("error"))) ||
                    (stepInfo.get("stepName").toString().toLowerCase().contains(("failure"))) ||
                    runIsCanceled
                    ) {
                color = "red";
            }

            resultBuilder.append("<font color=\"" + color + "\"><b>" + formatter.format(date) + "</b>").append("\n");
            resultBuilder.append("\t<b>stepPrimaryResult</b> : ").append(object.get("stepPrimaryResult").toString()).append("\n");

            resultBuilder.append("\t<b>status</b> : ").append(object.get("status").toString().replace("\"", "")).append("\n");
            resultBuilder.append("\t<b>rawResult</b> : ").append(object.get("rawResult").toString()).append("\n");
            resultBuilder.append("\t<b>stepResult</b> : ").append(object.get("stepResult").toString()).append("\n");
            resultBuilder.append("\t<b>stepInputs</b> : ").append(object.get("stepInputs").toString()).append("\n");

            resultBuilder.append("\t<b>stepName</b> : ").append(stepInfo.get("stepName").toString().replace("\"", "")).append("\n");
            resultBuilder.append("\t<b>flowName</b> : ").append(stepInfo.get("flowName").toString().replace("\"", "")).append("\n");
            resultBuilder.append("\t<b>type</b> : ").append(stepInfo.get("type").toString().replace("\"", "")).append("\n");
            resultBuilder.append("\t<b>transitionMessage</b> : ").append(stepInfo.get("transitionMessage").toString().replace("\"", "")).append("\n");
            resultBuilder.append("\t<b>responseType</b> : ").append(stepInfo.get("responseType").toString().replace("\"", "")).append("</font>\n");
        }

        return resultBuilder.toString();

    }

    private static void setJSONExecutionResult(String executionResult) {

        jsonExecutionResult = executionResult;
    }

    public static String getJsonExecutionResult() {
        return jsonExecutionResult;
    }
}