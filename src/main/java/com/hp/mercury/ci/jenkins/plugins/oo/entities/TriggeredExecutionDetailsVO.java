package com.hp.mercury.ci.jenkins.plugins.oo.entities;

/**
 * Created with IntelliJ IDEA.
 * User: levinger
 */

public class TriggeredExecutionDetailsVO {
    private String feedUrl;
    private String executionId;
    private String errorCode;

    public String getFeedUrl() {
        return feedUrl;
    }

    public TriggeredExecutionDetailsVO setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
        return this;
    }

    public String getExecutionId() {
        return executionId;
    }

    public TriggeredExecutionDetailsVO setExecutionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public TriggeredExecutionDetailsVO setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

}
