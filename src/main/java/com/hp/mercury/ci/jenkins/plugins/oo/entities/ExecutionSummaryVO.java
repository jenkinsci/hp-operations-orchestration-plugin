package com.hp.mercury.ci.jenkins.plugins.oo.entities;


/**
 * Created with IntelliJ IDEA.
 * User: levinger
 */
@SuppressWarnings("unused")
public class ExecutionSummaryVO {
    private String executionId;
    private String branchId;
    private String startTime;
    private String endTime;
    private ExecutionStatusState status;
    private ExecutionResult resultStatusType;
    private String resultStatusName;
    private String pauseReason;
    private String owner;
    private String triggeredBy;
    private String flowUuid;
    private String flowPath;
    private String executionName;
    private String triggeringSource;
    private String branchesCount;
    private String roi;

    @Override
    public String toString(){
        return "ExecutionId: [" +  executionId + "]\n" +
        "branchId: [" +  branchId + "]\n" +
        "startTime: [" +  startTime + "]\n" +
        "endTime: [" +  endTime + "]\n" +
        "ExecutionStatusState: [" +  status + "]\n" +
        "ExecutionResult: [" +  resultStatusType + "]\n" +
        "resultStatusName: [" +  resultStatusName + "]\n" +
        "pauseReason: [" +  pauseReason + "]\n" +
        "owner: [" +  owner + "]\n" +
        "triggeredBy: [" +  triggeredBy + "]\n" +
        "flowUuid: [" +  flowUuid + "]\n" +
        "flowPath: [" +  flowPath + "]\n" +
        "executionName: [" +  executionName + "]\n" +
        "triggeringSource: [" +  triggeringSource + "]\n" +
        "branchesCount: [" +  branchesCount + "]\n" +
        "roi: [" +  roi + "]";
    }


    public ExecutionSummaryVO() {
    }

    //getters & setters
    public String getExecutionId() {
        return executionId;
    }

    public ExecutionSummaryVO setExecutionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    public String getBranchId() {
        return branchId;
    }

    public ExecutionSummaryVO setBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public String getResultStatusName() {
        return resultStatusName;
    }

    public ExecutionSummaryVO setResultStatusName(String resultStatusName) {
        this.resultStatusName = resultStatusName;
        return this;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public ExecutionSummaryVO setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public ExecutionSummaryVO setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public ExecutionSummaryVO setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
        return this;
    }

    public String getFlowUuid() {
        return flowUuid;
    }

    public ExecutionSummaryVO setFlowUuid(String flowUuid) {
        this.flowUuid = flowUuid;
        return this;
    }

    public String getFlowPath() {
        return flowPath;
    }

    public ExecutionSummaryVO setFlowPath(String flowPath) {
        this.flowPath = flowPath;
        return this;
    }

    public String getExecutionName() {
        return executionName;
    }

    public ExecutionSummaryVO setExecutionName(String executionName) {
        this.executionName = executionName;
        return this;
    }

    public String getTriggeringSource() {
        return triggeringSource;
    }

    public ExecutionSummaryVO setTriggeringSource(String triggeringSource) {
        this.triggeringSource = triggeringSource;
        return this;
    }

    public String getBranchesCount() {
        return branchesCount;
    }

    public ExecutionSummaryVO setBranchesCount(String branchesCount) {
        this.branchesCount = branchesCount;
        return this;
    }

    public String getRoi() {
        return roi;
    }

    public ExecutionSummaryVO setRoi(String roi) {
        this.roi = roi;
        return this;
    }


    public String getStartTime() {
        return startTime;
    }

    public ExecutionSummaryVO setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public ExecutionSummaryVO setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    public ExecutionStatusState getStatus() {
        return status;
    }

    public ExecutionSummaryVO setStatus(ExecutionStatusState status) {
        this.status = status;
        return this;
    }


    public ExecutionResult getResultStatusType() {
        return resultStatusType;
    }

    public ExecutionSummaryVO setResultStatusType(ExecutionResult resultStatusType) {
        this.resultStatusType = resultStatusType;
        return this;
    }
}
