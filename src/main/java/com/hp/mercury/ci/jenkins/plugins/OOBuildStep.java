// (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.hp.mercury.ci.jenkins.plugins;


import com.hp.mercury.ci.jenkins.plugins.oo.core.oo10x.WaitExecutionResult;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionResult;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionStatusState;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.StepLog;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.TriggeredExecutionDetailsVO;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.MapValueCriteria;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.StringUtils;
import com.hp.mercury.ci.jenkins.plugins.oo.core.*;
import com.hp.mercury.ci.jenkins.plugins.oo.encryption.TripleDES;
import com.hp.mercury.ci.jenkins.plugins.oo.matcher.MatchStrategy;
import com.hp.mercury.ci.jenkins.plugins.oo.ssl.FakeSocketFactory;
import hudson.*;
import hudson.matrix.*;
import hudson.model.*;
import hudson.util.FormValidation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.CookieSpecRegistry;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.impl.client.DefaultHttpClient;

import com.hp.mercury.ci.jenkins.plugins.oo.utils.CollectionUtils;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.Criteria;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.Handler;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BestMatchSpec;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.impl.cookie.IgnoreSpecFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.springframework.util.Assert;

import javax.xml.bind.JAXB;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

import static hudson.model.Result.*;

public class OOBuildStep extends Builder {

    public static final List<String> RESULTS = CollectionUtils.map(
            Arrays.asList(UNSTABLE, FAILURE),
            new Handler<String, Result>() {

                public String apply(Result node) {
                    return node.toString();
                }
            }
    );
    private static byte[] encodedValue;

    public static List<String> possibleResults() {
        return RESULTS;
    }

    private enum DEV_MODE {}

    private String selectedFlow;
    private String ooServer;
    private List<OOArg> args;
    private Result desiredResultType;
    private String valueToCompareWith;
    private String stepExecutionTimeout;
    private String runName;
    private MatchStrategy matchStrategy;
    private String retVariableToCheck;
    private String basepath;
    // private Boolean changeBuildResult;
    private static String globalKeystore;
    private static final Log LOG = LogFactory.getLog(OOBuildStep.class);

    private boolean abortBuildOnRunFailure = true;

    //we don't store this in the descriptor because it contains authentication data and we don't want it to be serialized to xml...
    private static DefaultHttpClient httpClient = null;

    public static DefaultHttpClient getHttpClient() {

        final DescriptorImpl descriptor = getDescriptorStatically();

        if (httpClient == null && descriptor != null) {

            initializeHttpClient(descriptor);
        }
        return httpClient;
    }



    private static void initializeHttpClient(DescriptorImpl descriptor) {

        final int maxConnectionsPerRoute = 100;
        final int maxConnectionsTotal = 100;

        ThreadSafeClientConnManager threadSafeClientConnManager = new ThreadSafeClientConnManager();
        threadSafeClientConnManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        threadSafeClientConnManager.setMaxTotal(maxConnectionsTotal);

        httpClient = new DefaultHttpClient(threadSafeClientConnManager);
        if (descriptor.isIgnoreSsl()) {
            threadSafeClientConnManager.getSchemeRegistry().register(new Scheme("https", 443, new FakeSocketFactory()));
        } else if (descriptor.getKeystorePath() != null) {
            try {
                SSLSocketFactory sslSocketFactory = sslSocketFactoryFromCertificateFile(
                        descriptor.getKeystorePath(),
                        decrypt(descriptor.getKeystorePassword()).toCharArray());
                sslSocketFactory.setHostnameVerifier(new BrowserCompatHostnameVerifier());
                // For less strict rules in dev mode you can try
                //sslSocketFactory.setHostnameVerifier(new AllowAllHostnameVerifier());
                threadSafeClientConnManager.getSchemeRegistry().register(new Scheme("https", 443, sslSocketFactory));
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Could not register https scheme: ", e);
            } catch (KeyManagementException e) {
                LOG.error("Could not register https scheme: ", e);
            } catch (KeyStoreException e) {
                LOG.error("Could not register https scheme: ", e);
            } catch (UnrecoverableKeyException e) {
                LOG.error("Could not register https scheme: ", e);
            } catch (IOException e) {
                LOG.error("Could not load keystore file: ", e);
            } catch (CertificateException e) {
                LOG.error("Could not load keystore file: ", e);
            }
        }

        final HttpParams params = httpClient.getParams();
        final int timeoutInSeconds = descriptor.getTimeout() * 1000;
        HttpConnectionParams.setConnectionTimeout(params, timeoutInSeconds);
        HttpConnectionParams.setSoTimeout(params, timeoutInSeconds);
        HttpProtocolParams.setUseExpectContinue(httpClient.getParams(), false);

        for (OOServer s : descriptor.getOoServers(true).values()) {

            URL url = null;
            try {
                url = new URL(s.getUrl());
            } catch (MalformedURLException mue) {
                //can't happen, we pre-validate the URLS during configuration and set active to false if bad.
            }

            //check why it doesn't use the credentials provider
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM, "basic"),
                    new UsernamePasswordCredentials(
                            s.getUsername(),
                            decrypt(s.getPassword())
                    )
            );
        }

    }

    public static void setEncodedCredentials(String username, String password) {

        if (username == null || username.length() == 0){
            encodedValue = null;
        } else {

            String value = username + ":" + decrypt(password);
            encodedValue = Base64.encodeBase64(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static byte[] getEncodedCredentials(){
        return encodedValue;
    }

    private static SSLSocketFactory sslSocketFactoryFromCertificateFile(String keyStorePath, char[] password) throws
            KeyStoreException,
            IOException,
            NoSuchAlgorithmException,
            CertificateException,
            KeyManagementException,
            UnrecoverableKeyException {

        char[] ksPassword = password;
        FileInputStream fis = new FileInputStream(keyStorePath);

        //while similar to singleton patten in API, this provides a NEW instance.
        KeyStore trustore = KeyStore.getInstance("JKS");

        globalKeystore = keyStorePath;

        trustore.load(fis, ksPassword);
        final SSLSocketFactory sslSocketFactory = new SSLSocketFactory(trustore);

        return sslSocketFactory;
    }

    @DataBoundConstructor
    public OOBuildStep(
            String ooServer,
            String basepath,
            String selectedFlow,
            List<OOArg> args,
            String retVariableToCheck,
            int comparisonOrdinal,
            String valueToCompareWith,
            String desiredResultType,
            String stepExecutionTimeout,
            String runName) throws Descriptor.FormException {

        //this should never happen..
        if (ooServer == null || ooServer.isEmpty() || !OOAccessibilityLayer.getAvailableServers().contains(ooServer)) {
            throw new Descriptor.FormException("illegal OO server chosen. must be non null, non empty, " +
                    "pre-configured OO server label.", "ooServer");
        }

        //this should never happen..
        if (selectedFlow == null || selectedFlow.isEmpty()) {
            throw new Descriptor.FormException("illegal flow chosen. must be non null, non empty.", "selectedFlow");
        }

        //if basepath doesn't start with "/library", make it empty
        if (basepath == null || !StringUtils.unslashifyPrefix(basepath.toLowerCase()).startsWith("library")) {
            basepath = "";
        }

        this.ooServer = ooServer;
        this.selectedFlow = selectedFlow;
        this.args = args;
        this.basepath = basepath;
        this.stepExecutionTimeout = stepExecutionTimeout;
        this.runName = runName;
        setStepExecutionTimeout(stepExecutionTimeout);

        if (
                retVariableToCheck != null &&
                        valueToCompareWith != null &&
                        comparisonOrdinal != -1 &&
                        desiredResultType != null) {

            this.retVariableToCheck = retVariableToCheck;
            this.valueToCompareWith = valueToCompareWith;
            this.matchStrategy = matchStrategy.values()[comparisonOrdinal];
            this.desiredResultType = Result.fromString(desiredResultType);
        }
    }


    @SuppressWarnings("unused")
    //this method contains all "backwards-compatible" persistence logic.
    //it runs after the object is deserialized from xml. standard xstream and java serialization API.
    public Object readResolve() {

        //basepath property was added late, and we use it when listing flows from server.
        //if we deserialize an older configuration it will not have a basepath property, and basepath will be null.
        //list will therefore fail. we fix this by making sure that if we have an older configuration, where the value
        //is null - we give a good default which provides the current behavior - which is listing all flows.
        if (basepath == null) {
            basepath = "/Library";
        }

        return this;
    }

    public OOServer getOOServer() {

        return OOAccessibilityLayer.getOOServer(this.ooServer);
    }

    public OOFlow getOOFlow() {

        return new OOFlow(this.selectedFlow);
    }

    public List<OOArg> getArgs() {
        return args;
    }

    public Result getDesiredResultType() {
        return desiredResultType;
    }

    public String getValueToCompareWith() {
        return valueToCompareWith;
    }

    public MatchStrategy getMatchStrategy() {
        return matchStrategy;
    }

    public String getRetVariableToCheck() {
        return retVariableToCheck;
    }

    public static DescriptorImpl getDescriptorStatically() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        //for Musca, Lucian-Cristian who wanted an easy way to configure 10 minutes as default stepExecutionTimeout
        public static final int DEFAULT_TIMEOUT = 600;

        private Map<String, OOServer> ooServers;
        private Boolean ignoreSsl;
        private Integer timeout;
        private String keystorePassword;
        private String keystorePath;

        //used by config.jelly
        @SuppressWarnings("unused")
        public boolean isDevModeEnabled() {

            final String devMode = DEV_MODE.class.getName();
            return System.getProperty(devMode, null) != null;
        }

        public Integer getTimeout() {

            //null safe logic for when this class is first loaded  and no user config available
            return timeout == null ? DEFAULT_TIMEOUT : timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Map<String, OOServer> getOoServers(boolean filterOnlyActive) {

            if (ooServers == null) {
                ooServers = new HashMap<String, OOServer>();
            }

            Map<String, OOServer> copy = new HashMap<String, OOServer>(ooServers);

            if (filterOnlyActive) {
                CollectionUtils.filter(copy.entrySet(), new MapValueCriteria<String, OOServer>(
                        new Criteria<OOServer>() {

                            public boolean isSuccessful(OOServer tested) {
                                return tested.isActive();
                            }
                        }
                ));
            }

            return copy;
        }

        @SuppressWarnings("unused")//used from ui
        @JavaScriptMethod
        public Collection<String> listFlowsForServer(String uniqueLabel, String basepath) throws IOException {
            final OOServer server = ooServers.get(uniqueLabel);
            if (server == null) {
                throw new NullPointerException("You must select an OO server before listing its flows.");
            }
            final List<OOFlow> flows = server.getFlows(basepath);
            return CollectionUtils.map(flows, new Handler<String, OOFlow>() {

                public String apply(OOFlow node) {
                    return node.getId();
                }
            });
        }

        public boolean isIgnoreSsl() {

            //null safe logic for when this class is first loaded and no user configuration exists
            return ignoreSsl != null && ignoreSsl;
        }

        public void setIgnoreSsl(Boolean ignoreSsl) {
            this.ignoreSsl = ignoreSsl;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return ooServers != null && !ooServers.isEmpty();
        }

        @Override
        public String getDisplayName() {
            return "HP Operations Orchestration Flow Execution Build Step";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {

//            setStepExecutionTimeout(json.getInt("stepExecutionTimeout"));
            setIgnoreSsl(json.getBoolean("ignoreSsl"));
            setKeystorePassword(json.getString("keystorePassword"));
            setKeystorePath(json.getString("keystorePath"));

            final Object ooServersConfigs = json.get("ooServers");
            Collection<Exception> serversConfigurationExceptions = null;

            if (ooServersConfigs == null) {
                ooServers = Collections.emptyMap();
                serversConfigurationExceptions = Collections.emptyList();
            }

            //handle array
            else if (ooServersConfigs instanceof JSONArray) {
                final JSONArray ooServerConfigsArray = (JSONArray) ooServersConfigs;

                final int configsCount = ooServerConfigsArray.size();
                ooServers = new HashMap<String, OOServer>(configsCount);
                serversConfigurationExceptions = new ArrayList<Exception>(configsCount);

                for (int i = 0; i < configsCount; i++) {
                    JSONObject ooServerConfig = (JSONObject) ooServerConfigsArray.get(i);
                    Exception serverConfigurationException = handleSingleServerConfig(i, ooServerConfig);
                    if (serverConfigurationException != null) {
                        serversConfigurationExceptions.add(serverConfigurationException);
                    }
                }
            }

            //handle single instance
            else if (ooServersConfigs instanceof JSONObject) {

                final JSONObject ooServerConfig = (JSONObject) ooServersConfigs;
                ooServers = new HashMap<String, OOServer>(1);
                serversConfigurationExceptions = new ArrayList<Exception>(1);
                Exception serverConfigurationException = handleSingleServerConfig(0, ooServerConfig);
                if (serverConfigurationException != null) {
                    serversConfigurationExceptions.add(serverConfigurationException);
                }
            }

            save();
            initializeHttpClient(this);

            ((OOAdministrativeMonitor) Jenkins.getInstance().getAdministrativeMonitor(
                    OOAdministrativeMonitor.MONITOR_ID)).setUrlConfigurationErrors(serversConfigurationExceptions);

            return super.configure(req, json);
        }

        private FormException handleSingleServerConfig(int i, JSONObject ooServerConfig) throws FormException {

            FormException ret = null;

            final String uniqueLabel = ooServerConfig.getString("uniqueLabel");

            if (uniqueLabel == null || uniqueLabel.isEmpty()) {
                ret = new Descriptor.FormException("illegal ooserver label identifier, must not be null or empty.", "uniqueLabel");
            }

            if (ooServers.containsKey(uniqueLabel)) {
                ret = new FormException("error, name '" + uniqueLabel + "' is already in use.", "uniqueLabel");
            }

            final String inputUrl = ooServerConfig.getString("hostUrl");

            //make sure it's a valid URL
            try {
                new URL(inputUrl);
            } catch (MalformedURLException mue) {
                ret = new FormException("The URL " + inputUrl + " is not a valid URL.", "hostUrl");
            }

            final OOServer s = new OOServer(
                    uniqueLabel,
                    inputUrl,
                    ooServerConfig.getString("username"),
                    encrypt(ooServerConfig.getString("password")),
                    ret == null         //the configuration should be activated if there were no exceptions
            );

            // Check that the new new OO server about to add has a unique URL
            boolean isUniqueURL = true;
            for (String key : ooServers.keySet()) {
                if ((inputUrl != null) && (ooServers.get(key) != null) &&
                        (inputUrl.equals(ooServers.get(key).getUrl()))) {
                    isUniqueURL = false;
                    break;
                }
            }
            // Only add the OO server in case it points to a unique ULR(host, port or version must be different)
            if (isUniqueURL) {
                ooServers.put(s.getUniqueLabel(), s);
            }

            return ret;
        }

        //data isn't loaded properly from xml config file without this constructor
        public DescriptorImpl() {
            super();
            load();

            initializeHttpClient(this);
        }

        @SuppressWarnings("unused") //ajax validation from UI
        public FormValidation doCheckKeystorePassword(@QueryParameter String value,
                                                      @QueryParameter String path) {
            //decrypt doesn't do anything if the value is not encrypted in the first place.
            // if it's encrypted in the UI because it was loaded from previously saved configuration, we need to decrypt.
            value = decrypt(value);
            try {
                sslSocketFactoryFromCertificateFile(path, value.toCharArray());
            } catch (Exception e) {
                return FormValidation.error(e, "could not load certificate file.");
            }
            return FormValidation.ok();
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public void setKeystorePassword(String keystorePassword) {

            this.keystorePassword = encrypt(keystorePassword);
        }

        @SuppressWarnings("unused") //ajax validation from UI
        public FormValidation doCheckKeystorePath(@QueryParameter String value) {

            final File file = new File(value);

            if (!file.exists()) {
                return FormValidation.error("key store file '" + value + "' not found.");
            }
            if (!file.canRead()) {
                return FormValidation.error("key store file '" + value + "' not readable.");
            }
            if (!file.isAbsolute()) {
                return FormValidation.error("'" + value + "' is not an absolute path.");
            }
            if (file.isDirectory()) {
                return FormValidation.error("'" + value + "' is a directory, not a certificates file.");
            }

            return FormValidation.ok();
        }

        public void setKeystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
        }

        /**
         * @return the plugin's truststore absolute path or null if no additional trustore is defined
         */
        public String getKeystorePath() {
            return keystorePath == null || keystorePath.isEmpty()
                    ? null : keystorePath;
        }
    }


    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {

        return super.prebuild(build, listener);
    }

    public String getBasepath() {
        return basepath;
    }

    public void setBasepath(String basepath) {
        this.basepath = basepath;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        final List<OOArg> argsToUse = expand(getArgs(), (AbstractBuild<MatrixProject, MatrixBuild>) build, listener);



        Map<String,String> buildVariables = build.getBuildVariables();
        Set<String> keySet = buildVariables.keySet();

        OOServer selectedOOServer = getOOServer();
        String selectedFlowS=this.selectedFlow;
        for (String key : keySet) {

            if (key.startsWith("i_")) {
                argsToUse.add(new OOArg(key.substring(2), buildVariables.get(key)));
                listener.getLogger().println("adding input " + key.substring(2) + " with value " + buildVariables.get(key) + " from the matrix axis");
            }

            if (key.startsWith("s_")) {
                selectedOOServer = OOAccessibilityLayer.getOOServer(buildVariables.get(key));
            }

            if (key.startsWith("f_")) {
                selectedFlowS = basepath + "/" + buildVariables.get(key);
            }
        }

        Assert.isTrue(selectedOOServer != null, "HP Operations Orchestration Server must be selected");

        listener.getLogger().println("Connected to central with label: [" + selectedOOServer.getUniqueLabel() + "], URL: ["+selectedOOServer.getUrl()+"]");
        URL url = new URL(selectedOOServer.getUrl());

        String urlString = StringUtils.slashify(selectedOOServer.getUrl().replace("PAS", "")) + "oo/rest/v1";

        final DescriptorImpl descriptor = getDescriptorStatically();

        if (selectedOOServer.getUsername() != null && selectedOOServer.getUsername().length() > 0) {
            listener.getLogger().println("Authentication will be used, username: " + selectedOOServer.getUsername());
            setEncodedCredentials(selectedOOServer.getUsername(), selectedOOServer.getPassword());
        } else {
            setEncodedCredentials(null, null);
        }

        // get version from Central
        Boolean is9x = OOAccessibilityUtils.isOoVersionLowerThen1010(urlString + "/version");

        // log version
        //listener.getLogger().println("Central version: " + version);
        //listener.getLogger().println("Global Keystore: " + globalKeystore);

        listener.getLogger().println(
                "Running " + selectedFlowS +
                        " on " + selectedOOServer.getUniqueLabel() +
                        " with parameters " + argsToUse
        );


        // legacy invoke of a flow
        if (is9x) {
            OORunRequest runRequest = null;
            OORunResponse ooBuildStepResult = null;
            try {
                runRequest = new OORunRequest(
                        selectedOOServer,
                        new OOFlow(selectedFlowS),
                        argsToUse);

                ooBuildStepResult = OOAccessibilityLayer.run(runRequest);
            } catch (Exception e) {
                e.printStackTrace(listener.getLogger());
                if (abortBuildOnRunFailure) {
                    throw new AbortException("build step failed: " + e.getMessage());
                }
            }

            if (ooBuildStepResult != null) {

                listener.getLogger().println("flow results:");
                JAXB.marshal(ooBuildStepResult, listener.getLogger());

                final EnvVars envVars = mapReturnValues(ooBuildStepResult, listener);
                build.addAction(new OOBuildStepResultsEnvironmentInjectionAction(envVars));

                if (ooBuildStepResult.hasReport()) {
//                    build.addAction(new OOBuildStepResultReportAction(runRequest.getFlow().getId(), ooBuildStepResult.getReportUrl()));
                    build.addAction(new OOBuildStepResultReportAction(ooBuildStepResult.getReportUrl()));
                }
                listener.getLogger().println("***" + getRetVariableToCheck() + "***");

                if (getRetVariableToCheck().length()>0) {

                    final String serverReturnedValue = envVars.get(getRetVariableToCheck());
                    if (serverReturnedValue == null) {
                        throw new AbortException("could not check value of variable '" + getRetVariableToCheck() +
                                "' - flow execution returned no such variable from the server.\n");
                    } else if (getMatchStrategy().matches(serverReturnedValue, getValueToCompareWith())) {
                        listener.getLogger().println("changing status of build to " + getDesiredResultType() + " because " +
                                "value of " +
                                getRetVariableToCheck() + " (=" + serverReturnedValue + ") " +
                                getMatchStrategy() + " " + getValueToCompareWith() + "\n");

                        build.setResult(getDesiredResultType());

                        if (getDesiredResultType().equals(Result.FAILURE)) {
                            throw new AbortException("Aborting the build because flow failed and FAILURE was chosen.\n");
                        }
                    }

                }


                //Set build status according to run status
                if (envVars.get("flowReturnCode").equals("Error")){
                    listener.annotate(new SimpleHtmlNote("<font color=\"red\"><b>Run ended with status ["+envVars.get("flowReturnCode")+"] changing the build result to "+Result.FAILURE+"</b></font>\n"));
                    build.setResult(Result.FAILURE);
                }else if (ExecutionResult.NO_ACTION_TAKEN.equals(envVars.get("flowReturnCode").equals("No Action"))){
                    listener.annotate(new SimpleHtmlNote("<font color=\"yellow\"><b>Run ended with status ["+envVars.get("flowReturnCode")+"] changing the build result to "+Result.UNSTABLE+"</b></font>\n"));
                    build.setResult(Result.UNSTABLE);
                }


            }
        } else {

            // post 10.10 invoke of a flow
            // get UUID of the selected flow

            String selectedFlowUUID = OOAccessibilityUtils.getFlowID10x(selectedFlowS, urlString + "/flows/library");
            listener.getLogger().println("The flow: " + selectedFlowS + " with UUID " + selectedFlowUUID + " is executing...");
            listener.getLogger().getClass();
            OOAccessibilityLayer ooAccessibilityLayer = new OOAccessibilityLayer();

            String runName = resolveParametersInString(build, listener, this.runName);

            //trigger the flow
            TriggeredExecutionDetailsVO triggeredExecutionDetailsVO = ooAccessibilityLayer.run10x(selectedFlowUUID, argsToUse, urlString, stepExecutionTimeout, listener, runName);

            //Create report with link to run on OO
            build.addAction(new OOBuildStepResultReportAction(ooAccessibilityLayer.runURL));

            //Wait for completion
            WaitExecutionResult waitExecutionResult = ooAccessibilityLayer.waitExecutionComplete(triggeredExecutionDetailsVO, stepExecutionTimeout, listener, urlString);

            //get step log
            List<StepLog> stepLog = ooAccessibilityLayer.getStepLog(waitExecutionResult.getStepCount(),triggeredExecutionDetailsVO.getFeedUrl());

            //Handle build result
            if (waitExecutionResult.isTimedOut()){//if timed out
                listener.annotate(new SimpleHtmlNote("\n<font color=\"red\"><b>Timeout failure: The execution was not completed in the allotted time.\nLast status was: "+waitExecutionResult.getLastExecutionStatus()+" </b></font> \n\n"));
                build.setResult(Result.FAILURE);
            }else{//No time out
                listener.getLogger().println("Run ended with status ["+waitExecutionResult.getFullStatusName()+"]");
                if (!ExecutionStatusState.COMPLETED.equals(waitExecutionResult.getLastExecutionStatus())){//If flow wasn't completed
                    listener.annotate(new SimpleHtmlNote("<font color=\"red\"><b>Run was not completed, changing the build result to "+Result.FAILURE+"</b></font>\n"));

                    build.setResult(Result.FAILURE);
                }else{//flow was completed
                    //Set build status according to run outputs
                    if (stepLog.size()>0){
                        StepLog lastStep = stepLog.get(stepLog.size()-1);
                        Map<String,String> lastRawResults = lastStep.getRawResult();
                        if (lastRawResults!=null && lastRawResults.get(getRetVariableToCheck())!=null && !getRetVariableToCheck().isEmpty()){ //if run has the expected output
                            if (getMatchStrategy().matches(lastRawResults.get(getRetVariableToCheck()), getValueToCompareWith())) {
                                listener.getLogger().println("changing status of build to " + getDesiredResultType() + " because " +
                                        "value of " +
                                        getRetVariableToCheck() + " (" + lastRawResults.get(getRetVariableToCheck()) + ") " +
                                        getMatchStrategy() + " expected value of " + getValueToCompareWith());

                                build.setResult(getDesiredResultType());
                            }
                        }else if (!getRetVariableToCheck().isEmpty()){ //flow output is empty or output wasn't found
                            listener.annotate(new SimpleHtmlNote("<font color=\"red\"><b>could not check value of variable '" + getRetVariableToCheck() +
                                    "' - flow execution returned no such variable from the server.</b></font>\n"));
                        }
                    }
                    //Set build status according to run status
                    if (ExecutionResult.ERROR.equals(waitExecutionResult.getLastExecutionResult())){
                        //Print stack trace
                        List<StepLog> stackTrace = ooAccessibilityLayer.createStackTrace(stepLog, "0");
                        listener.annotate(new SimpleHtmlNote(ooAccessibilityLayer.stackTraceAsString(stackTrace)));

                        listener.annotate(new SimpleHtmlNote("<font color=\"red\"><b>Run ended with status ["+waitExecutionResult.getLastExecutionResult()+"] changing the build result to "+Result.FAILURE+"</b></font>\n"));
                        build.setResult(Result.FAILURE);
                    }else if (ExecutionResult.NO_ACTION_TAKEN.equals(waitExecutionResult.getLastExecutionResult())){
                        listener.annotate(new SimpleHtmlNote("<font color=\"yellow\"><b>Run ended with status ["+waitExecutionResult.getLastExecutionResult()+"] changing the build result to "+Result.UNSTABLE+"</b></font>\n"));
                        build.setResult(Result.UNSTABLE);
                    }
                }
            }

            //print the steps (convert step log to json)
//            listener.annotate(new SimpleHtmlNote(ooAccessibilityLayer.formatResult(jsonExecutionResult)));

        }

        return true;
    }

    private List<OOArg> expand(List<OOArg> args, AbstractBuild<MatrixProject, MatrixBuild> build, BuildListener listener) throws IOException, InterruptedException {

        if (args != null) {
            List<OOArg> expanded = new ArrayList<OOArg>(args.size());
            EnvVars env = build.getEnvironment(listener);

            AbstractProject project = build.getProject();
            listener.getLogger().println("Project name" + project.getDisplayName());

            Map<AbstractProject,Integer> map= build.getUpstreamBuilds();

            for (OOArg arg : args) {

                final String name = arg.getName();
                final String value = arg.getValue();

                final String nameExpansion = env.expand(name);
                final String valueExpansion = env.expand(value);

                final OOArg expandedArg =
                        nameExpansion.equals(name) && valueExpansion.equals(value) ?
                                arg :
                                new OOArg(nameExpansion, valueExpansion);

                expanded.add(expandedArg);
            }

            return expanded;
        } else {
            // we need an arg even for a no-arg run : QCCR 24513
            args = new ArrayList<OOArg>();
            //args.add(0, new OOArg("defaultParamName", "defaultParamValue"));
        }
        return args;
    }

    private EnvVars mapReturnValues(OORunResponse ooBuildStepResult, BuildListener listener) {
        EnvVars vars = new EnvVars();
        for (OORunResponse.OORunReturnItem item : ooBuildStepResult.getItems()) {
            final String name = item.getName();
            final String value = item.getValue();
            listener.getLogger().println("assigning: " + name + " = " + value);
            vars.put(name, value);
        }
        return vars;
    }

    public static URI URI(String urlString) {

        try {
            final URL url = new URL(urlString);

            return new URI(
                    url.getProtocol(),
                    null,
                    url.getHost(),
                    url.getPort(),
                    url.getPath(),
                    url.getQuery(),
                    null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String encrypt(String password) {

        return isEncrypted(password) ?
                password :
                "MD5:" + TripleDES.encryptPassword(password);

    }

    private static String decrypt(String password) {

        return isEncrypted(password) ?
                TripleDES.decryptPassword(password.substring("MD5:".length())) :
                password;
    }

    private static boolean isEncrypted(String password) {
        return password.startsWith("MD5:");
    }

    public void setStepExecutionTimeout(String stepExecutionTimeout){
        this.stepExecutionTimeout = stepExecutionTimeout;
    }

    public String getStepExecutionTimeout(){
        return stepExecutionTimeout;
    }

    public String getRunName(){
        return runName;
    }


    protected String resolveParametersInString(Run<?, ?> build, TaskListener listener, String input) {
        try {
            return build.getEnvironment(listener).expand(input);
        } catch (Exception e) {
            listener.getLogger().println("Failed to resolve parameters in string \""+
                    input+"\" due to following error:\n"+e.getMessage());
        }
        return input;
    }

}
