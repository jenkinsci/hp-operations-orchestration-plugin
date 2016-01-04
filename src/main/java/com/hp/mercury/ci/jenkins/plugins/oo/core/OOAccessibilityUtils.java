package com.hp.mercury.ci.jenkins.plugins.oo.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hp.mercury.ci.jenkins.plugins.OOBuildStep;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: golgot
 * Date: 11/25/14
 */
public class OOAccessibilityUtils {

    private static final JsonParser parser = new JsonParser();
    private static final DefaultHttpClient client = OOBuildStep.getHttpClient();

    public static String getFlowID10x(String selectedFlow, String urlString) throws IOException {

        final URI uri = OOBuildStep.URI(urlString);
        final HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Content-Type", "application/json");
        if (OOBuildStep.getEncodedCredentials()!=null) {
            httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        if(selectedFlow.substring(0,1).contains("/"))
            selectedFlow = selectedFlow.replaceFirst("/","").trim();

        HttpResponse response = client.execute(httpGet);
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();

        JsonArray array = (JsonArray) (parser.parse(convertStreamToString(is)));
        String id = "";
        JsonObject object;
        for (int i = 0; i < array.size(); i++) {
            object = (JsonObject) array.get(i);
            if (object.get("path").getAsString().replace("\"", "").equals(selectedFlow + ".xml")) {
                id = object.get("id").getAsString();
                break;
            }
        }
        return id;
    }

    public static String getOOServerVersion(String urlString) throws IOException {

        JsonObject element;

        final URI uri = OOBuildStep.URI(urlString);
        final HttpGet httpGet = new HttpGet(uri);

        if (OOBuildStep.getEncodedCredentials()!=null) {
            httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        HttpResponse response = client.execute(httpGet);
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();

        element = (JsonObject) (parser.parse(convertStreamToString(is)));

        return element.get("version").getAsString();

    }

    public static boolean isOoVersionLowerThen1010(String urlString) throws IOException {
        JsonObject element;

        final URI uri = OOBuildStep.URI(urlString);
        final HttpGet httpGet = new HttpGet(uri);

        if (OOBuildStep.getEncodedCredentials()!=null) {
            httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        HttpResponse response = client.execute(httpGet);
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();

        if (response.getStatusLine().getStatusCode()==404){
            return true;
        }

        element = (JsonObject) (parser.parse(convertStreamToString(is)));

        if (element.get("version").getAsString().compareTo("10.10") < 0){
            return true;
        }else{
            return false;
        }
    }

    protected static String getStringFromResponse(HttpGet httpGet) throws IOException {

        HttpResponse response2;
        HttpEntity entity2;

        if (OOBuildStep.getEncodedCredentials()!=null) {
            httpGet.addHeader("Authorization", "Basic " + new String(OOBuildStep.getEncodedCredentials()));
        }

        response2 = client.execute(httpGet);

        entity2 = response2.getEntity();
        InputStream is = entity2.getContent();
        return convertStreamToString(is);

    }

    public static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder stringBuilder = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return stringBuilder.toString();
    }

    protected static boolean isExecutionComplete(String s) {

        boolean executionComplete = false;   // is the execution complete ? We assume not

        try {
            JsonArray array = (JsonArray) (parser.parse(s));
            JsonObject object;

            // we need a step STATUS of "COMPLETED" and a step TYPE of "RETURN_STEP" for the last step in the main flow ( its step PATH will only contain 1 dot )
            // this means quite strong dependency on the OO 10 API. If they change it, this code is screwed, and it will not work

            object = (JsonObject) array.get(array.size() - 1);
            JsonElement status = object.get("status");  // status property is outside of the actual stepInfo object
            JsonElement stepInfo = object.get("stepInfo");

            JsonObject infoObj = (JsonObject) (stepInfo);
            JsonElement type = infoObj.get("type");     // type property is inside of the actual stepInfo object
            JsonElement path = infoObj.get("path");     // path property is inside of the actual stepInfo object

            if ((path != null) && (status != null) && (type != null)) {
                if ((path.getAsString().split("\\.").length == 2) && (status.getAsString().equals("COMPLETED")) && (type.getAsString().equals("RETURN_STEP"))) {
                    executionComplete = true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return executionComplete;
    }

    protected static String getExecutionStatus(String s) {

        try {

            JsonArray array = (JsonArray) (parser.parse(s));
            JsonObject object;

            object = (JsonObject) array.get(0);
            JsonElement status = object.get("status");  // status property is outside of the actual stepInfo object
            return status.getAsString();
        } catch (Exception e){
            return "";
        }


    }

}
