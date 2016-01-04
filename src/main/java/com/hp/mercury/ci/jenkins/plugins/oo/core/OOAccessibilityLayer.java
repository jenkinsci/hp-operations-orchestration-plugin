// (c) Copyright 2013 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.hp.mercury.ci.jenkins.plugins.oo.core;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.hp.mercury.ci.jenkins.plugins.oo.core.oo10x.WaitExecutionResult;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionStatusState;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionSummaryVO;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionVO;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.RecordBoundInputVO;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.StepLog;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.TriggeredExecutionDetailsVO;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.StringUtils;
import com.hp.mercury.ci.jenkins.plugins.oo.http.JaxbEntity;
import com.hp.mercury.ci.jenkins.plugins.OOBuildStep;
import hudson.model.BuildListener;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.springframework.util.Assert;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class OOAccessibilityLayer {

    private static final String REST_SERVICES_URL_PATH = "services/rest/";
    private static final String LIST_OPERATION_URL_PATH = "list/";
    private static final String RUN_OPERATION_URL_PATH = "run/";
    private static final String EXECUTIONS_API = "/executions/";
    private final static String EXECUTE_SUMMARY_POSTFIX = "/summary/";
    private final static String EXECUTION_STEPS_COUNT_POSTFIX = "/steps/count";

    private static final DefaultHttpClient client = OOBuildStep.getHttpClient();
    private static final JsonParser parser = new JsonParser();
    private static final Gson gson = new Gson();

    public String feedURL;
    public String runURL;
    public String runId;

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
//        if (OOBuildStep.getEncodedCredentials()!=null) {
//            httpPost.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
//        }

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

    public TriggeredExecutionDetailsVO run10x(String selectedFlowUUID, List<OOArg> argsToUse, String urlString, String timeout, BuildListener listener, String runName) throws IOException, InterruptedException {

        final HttpPost httpPost = new HttpPost(urlString + EXECUTIONS_API);

        //Convert inputs to the required type
        Map<String, String> inputs = new HashMap<String, String>();
        for (OOArg arg : argsToUse) {
            inputs.put(arg.getName(), arg.getValue());
        }

        // Create the POST object and add the parameters
        ExecutionVO executionVO = new ExecutionVO();
        executionVO.setUuid(selectedFlowUUID);
        if (runName != null && !runName.equals("")) executionVO.setRunName(runName);
        executionVO.setInputs(inputs);
        String body = gson.toJson(executionVO);
        StringEntity entity = new StringEntity(body, "UTF-8");
        httpPost.setEntity(entity);

        //Authenticate
        if (OOBuildStep.getEncodedCredentials()!=null) {
            httpPost.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        //Add relevant headers
        httpPost.setHeader("Content-Type", "application/json");   // this is mandatory in order for the request to work

        //Handle CSRF tokens cookies
        client.setCookieStore(handleCsrfCookies(client.getCookieStore()));

        //Execute the request
        HttpResponse response = client.execute(httpPost);

        //Verify the response is 201
        Assert.isTrue(response.getStatusLine().getStatusCode() == 201, "Invalid response code [" + response.getStatusLine().getStatusCode() + "]");

        //Read the response body
        String responseBody = EntityUtils.toString(response.getEntity());

//        HttpEntity responseEntity = response.getEntity();
//        InputStream is = responseEntity.getContent();

        //Extract data from response
//        JsonObject element = (JsonObject) (parser.parse(OOAccessibilityUtils.convertStreamToString(is)));
        TriggeredExecutionDetailsVO triggeredExecutionDetailsVO = gson.fromJson(responseBody, TriggeredExecutionDetailsVO.class);

        runId = triggeredExecutionDetailsVO.getExecutionId();
        listener.getLogger().println("Run ID : " + runId);
        runURL = triggeredExecutionDetailsVO.getFeedUrl().split("/oo/")[0] + "/oo/#/runtimeWorkspace/runs/" + triggeredExecutionDetailsVO.getExecutionId();

        listener.getLogger().println("Execution URL " + runURL);

        return triggeredExecutionDetailsVO;
    }


    public WaitExecutionResult waitExecutionComplete (TriggeredExecutionDetailsVO triggeredExecutionDetailsVO, String timeout, BuildListener listener, String urlString) throws InterruptedException, IOException {
        feedURL = triggeredExecutionDetailsVO.getFeedUrl();

        //close connection
//        is.close();

        //Read run details
        long defaultTimeout = 600000;
        Long timeoutLong = timeout.isEmpty() ? defaultTimeout : Long.parseLong(timeout);
        listener.getLogger().println("Flow execution timeout : " + timeoutLong + " ms");



        //Wait the run will completed
        List<ExecutionSummaryVO> executionSummary;
        ExecutionStatusState executionStatusState;
        int prevStepCount = 0;
        Boolean newLine = false;
        do {
            Long sleepIntervalInMs = timeoutLong>5000 ? 5000 : timeoutLong;
            timeoutLong -= (sleepIntervalInMs);
            Thread.sleep(sleepIntervalInMs);

            //Get run details
            HttpGet httpGet = new HttpGet(urlString + EXECUTIONS_API + runId + EXECUTE_SUMMARY_POSTFIX);

            //Handle CSRF tokens cookies
            client.setCookieStore(handleCsrfCookies(client.getCookieStore()));

            //Authenticate
            if (OOBuildStep.getEncodedCredentials()!=null) {
                httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
            }

            //Execute the request
            HttpResponse response = client.execute(httpGet);

            //Verify the response is OK
            Assert.isTrue(response.getStatusLine().getStatusCode() == 200, "Invalid response code [" + response.getStatusLine().getStatusCode() + "]");

            String responseBody = EntityUtils.toString(response.getEntity());

            executionSummary = gson.fromJson(responseBody, new TypeToken<List<ExecutionSummaryVO>>() {
            }.getType());
            executionStatusState = executionSummary.get(0).getStatus();

            //get steps details for steps progress
            httpGet = new HttpGet(urlString + EXECUTIONS_API + runId + EXECUTION_STEPS_COUNT_POSTFIX);

            //Handle CSRF tokens cookies
            client.setCookieStore(handleCsrfCookies(client.getCookieStore()));

            //Authenticate
            if (OOBuildStep.getEncodedCredentials()!=null) {
                httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
            }

            //Execute the request
            response = client.execute(httpGet);

            //Verify the response is OK
            Assert.isTrue(response.getStatusLine().getStatusCode() == 200, "Invalid response code [" + response.getStatusLine().getStatusCode() + "]");

            responseBody = EntityUtils.toString(response.getEntity());

            //printing steps progress
            int currentStepCount = gson.fromJson(responseBody, new TypeToken<Integer>() {
            }.getType());

            if (currentStepCount > prevStepCount) {
                if (newLine){
                    listener.getLogger().println();
                }
                listener.getLogger().println(currentStepCount + " steps were completed");
                newLine=false;
            } else {
                listener.getLogger().print(".");
                newLine = true;
            }

            prevStepCount = currentStepCount;

        } while (timeoutLong > 0 &&
                (executionStatusState.equals(ExecutionStatusState.RUNNING) || executionStatusState.equals(ExecutionStatusState.PENDING_PAUSE)));



        //get full status name
        String runResult;
        if (executionStatusState.equals(ExecutionStatusState.COMPLETED)){
            runResult = executionStatusState + " - " + executionSummary.get(0).getResultStatusType();
        }else{
            //TODO handle also paused runs
            runResult = executionStatusState.name();
        }

        WaitExecutionResult waitExecutionResult = new WaitExecutionResult();
        waitExecutionResult.setLastExecutionResult(executionSummary.get(0).getResultStatusType());
        waitExecutionResult.setLastExecutionStatus(executionStatusState);
        waitExecutionResult.setTimedOut(timeoutLong <= 0 &&
                (executionStatusState.equals(ExecutionStatusState.RUNNING) || executionStatusState.equals(ExecutionStatusState.PENDING_PAUSE)));
        waitExecutionResult.setStepCount(prevStepCount);

        return waitExecutionResult;
    }




    public List<StepLog> getStepLog(Long stepCount, String feedURL) throws IOException {
        //Get steps details
        HttpGet httpGet = new HttpGet(feedURL + "?pageSize="+stepCount); // hack !!!

        //Handle CSRF tokens cookies
        client.setCookieStore(handleCsrfCookies(client.getCookieStore()));

        //Authenticate
        if (OOBuildStep.getEncodedCredentials()!=null) {
            httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        //Execute the request
        HttpResponse response = client.execute(httpGet);

        //Verify the response is OK
        Assert.isTrue(response.getStatusLine().getStatusCode() == 200, "Invalid response code ["+response.getStatusLine().getStatusCode()+"]");

        String responseBody = EntityUtils.toString(response.getEntity());

        List<StepLog> stepLog = gson.fromJson(responseBody,  new TypeToken<List<StepLog>>() {}.getType());
//        List<StepLog> stackTrace = createStackTrace(stepLog, "0");
//        listener.annotate(new SimpleHtmlNote(stackTraceAsString(stackTrace)));

//        setJSONExecutionResult(responseBody);
//        responseBody = formatResult(responseBody);

        return stepLog;
//        return "";
    }

    public String stackTraceAsString(List<StepLog> stackTrace){
        String stackTraceString = "\n<font color=\"red\"><b>";

        String indent = "   ";
        String indentation = indent;

        for (StepLog stepLog : Lists.reverse(stackTrace)){
            stackTraceString+=stepLog.getStepInfo().getStepName() + " - " + stepLog.getStepInfo().getResponseType() + "\n"+indentation;
            indentation = indentation + indent;
        }

        StepLog lastStep = stackTrace.get(stackTrace.size()-1);
        indentation = indentation.substring(0,indentation.length()-1-indent.length());
        stackTraceString+="\n"+indentation;

        //Add raw results
        stackTraceString+="RAW Results: " + lastStep.getRawResult() + "\n" + indentation;

        //Add step inputs
        Map<String,String> stepInputs = new HashMap<String, String>();
        for(RecordBoundInputVO recordBoundInputVO : lastStep.getStepInputs()){
            stepInputs.put(recordBoundInputVO.getName(),recordBoundInputVO.getValue());
        }
        stackTraceString+="Step Inputs: " + stepInputs + "\n";

        stackTraceString+="</b></font>\n\n";
        return stackTraceString;
    }

    public List<StepLog> createStackTrace(List<StepLog> stepLogList, String startIndex){
        StepLog lastStep = null;
        StepLog prevLastStep = null;
        int maxLocalPathSize = 0;

        //find the last step
        for (StepLog stepLog : stepLogList) {
            String path = stepLog.getStepInfo().getPath();
            if (path.matches("^" + startIndex + ".\\d*")) {
                String[] splitPath = path.split("\\.");
                int currentLocalPath = Integer.parseInt(splitPath[splitPath.length - 1]);
                if (currentLocalPath >= maxLocalPathSize) {
                    maxLocalPathSize = currentLocalPath;
                    prevLastStep=lastStep;
                    lastStep = stepLog;
                }
            }
        }

        List<StepLog> stackTrace;
        if (lastStep != null) {
            stackTrace = createStackTrace(stepLogList, prevLastStep.getStepInfo().getPath());  //recursive, find the child last step
        }else{
            return new ArrayList<StepLog>();  //no more steps inside
        }
        stackTrace.add(prevLastStep);  //add current last step to stack trace
        return stackTrace;
    }

    public String formatResult(String executionResult) {

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

    private static CookieStore handleCsrfCookies(CookieStore cookieStore){
        List<Cookie> cookies = new ArrayList<Cookie>();
        for (Cookie cookie : cookieStore.getCookies()){
            if (!cookie.getName().contains("CSRF")){
                cookies.add(cookie);
            }
        }

        cookieStore.clear();

        for (Cookie cookie : cookies){
            cookieStore.addCookie(cookie);
        }
        return cookieStore;
    }


    private static void setJSONExecutionResult(String executionResult) {

        jsonExecutionResult = executionResult;
    }

    public static String getJsonExecutionResult() {
        return jsonExecutionResult;
    }
}