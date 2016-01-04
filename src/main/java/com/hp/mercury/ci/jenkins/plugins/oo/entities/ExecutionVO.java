package com.hp.mercury.ci.jenkins.plugins.oo.entities;


import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: levinger
 */

public class ExecutionVO {
    private String uuid;
    private Map<String, String> inputs;
    private String logLevel = "DEBUG";
    private String runName;

    //todo:override toString

    //getters & setters
    public String getUuid() {
        return uuid;
    }

    public ExecutionVO setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public ExecutionVO setLogLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public String getRunName() {
        return runName;
    }

    public ExecutionVO setRunName(String runName) {
        this.runName = runName;
        return this;
    }


    public Map<String, String> getInputs() {
        return inputs;
    }

    public ExecutionVO setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
        return this;
    }

}
