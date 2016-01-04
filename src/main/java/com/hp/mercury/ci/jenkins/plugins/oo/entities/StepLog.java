package com.hp.mercury.ci.jenkins.plugins.oo.entities;


import java.util.HashMap;
import java.util.List;

/**
 * User: neumane
 * Date: 01/04/14
 */
public class StepLog {
    @SuppressWarnings("unused")
    private StepInfo stepInfo;
    @SuppressWarnings("unused")
    private StepTransitionLog stepTransitionLog;
    @SuppressWarnings("unused")
    private String description;
    @SuppressWarnings("unused")
    private String stepPrimaryResult;
    @SuppressWarnings("unused")
    private String operationGroup;
    @SuppressWarnings("unused")
    private List<Object> errorList;
    @SuppressWarnings("unused")
    private List<RecordBoundInputVO> stepInputs;
    @SuppressWarnings("unused")
    private HashMap<String, String> rawResult;
    @SuppressWarnings("unused")
    private HashMap<String, String> stepResult;
    @SuppressWarnings("unused")
    private HashMap<String, String> extraData;
    @SuppressWarnings("unused")
    private String executionId;
    @SuppressWarnings("unused")
    private ExecutionEnums.StepStatus status;
    @SuppressWarnings("unused")
    private String workerId;
    @SuppressWarnings("unused")
    private String user;
    @SuppressWarnings("unused")
    private ExecutionEnums.StepLogCategory stepLogCategory;

    public StepInfo getStepInfo() {
        return stepInfo;
    }

    public StepTransitionLog getStepTransitionLog() {
        return stepTransitionLog;
    }

    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public String getStepPrimaryResult() {
        return stepPrimaryResult;
    }

    public String getOperationGroup() {
        return operationGroup;
    }

    public List<Object> getErrorList() {
        return errorList;
    }

    @SuppressWarnings("unused")
    public List<RecordBoundInputVO> getStepInputs() {
        return stepInputs;
    }

    public HashMap<String, String> getStepResult() {
        return stepResult;
    }

    public HashMap<String, String> getRawResult() {
        return rawResult;
    }

    @SuppressWarnings("unused")
    public HashMap<String, String> getExtraData() {
        return extraData;
    }

    public ExecutionEnums.StepStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionEnums.StepStatus status) {
        this.status = status;
    }

	public String getWorkerId() {
		return workerId;
	}

	public void setWorkerId(String workerId) {
		this.workerId = workerId;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}


}
