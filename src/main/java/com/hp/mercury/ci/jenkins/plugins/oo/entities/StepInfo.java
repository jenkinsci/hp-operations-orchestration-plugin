package com.hp.mercury.ci.jenkins.plugins.oo.entities;

import java.util.Set;

/**
 * User: neumane
 * Date: 01/04/14
 */
@SuppressWarnings("unused")
public class StepInfo {
    private String stepId;
    private String stepName;
    private String path;
    private String responseType;
    private Long startTime;
    private Long endTime;
    private boolean paused;
    private String orderNumber;
    private Set<String> invokedIds;
    private String flowName;
    private String flowId;
    private String type;
    private Long updatedTime;

    public String getResponseType() {
        return responseType;
    }

    public String getStepId() {
        return stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public String getPath() {
        return path;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Set<String> getInvokedIds() {
        return invokedIds;
    }

    public void setInvokedIds(Set<String> invokedIds) {
        this.invokedIds = invokedIds;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public Long getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Long updatedTime) {
		this.updatedTime = updatedTime;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	@Override
	public String toString() {
		return "StepInfo{" +
				"stepId='" + stepId + '\'' +
				", stepName='" + stepName + '\'' +
				", path='" + path + '\'' +
				", responseType='" + responseType + '\'' +
				", startTime=" + startTime +
				", endTime=" + endTime +
				", paused=" + paused +
				", orderNumber='" + orderNumber + '\'' +
				", flowName='" + flowName + '\'' +
				", invokedIds='" + invokedIds + '\'' +
				", flowId='" + flowId + '\'' +
				", type='" + type + '\'' +
				", updatedTime=" + updatedTime +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof StepInfo)) {
			return false;
		}

		StepInfo stepInfo = (StepInfo) o;

		if (paused != stepInfo.paused) {
			return false;
		}
		if (endTime != null ? !endTime.equals(stepInfo.endTime) : stepInfo.endTime != null) {
			return false;
		}
        if (invokedIds != null ? !invokedIds.equals(stepInfo.invokedIds) : stepInfo.invokedIds != null) {
            return false;
        }
		if (flowId != null ? !flowId.equals(stepInfo.flowId) : stepInfo.flowId != null) {
			return false;
		}
		if (flowName != null ? !flowName.equals(stepInfo.flowName) : stepInfo.flowName != null) {
			return false;
		}
		if (orderNumber != null ? !orderNumber.equals(stepInfo.orderNumber) : stepInfo.orderNumber != null) {
			return false;
		}
		if (path != null ? !path.equals(stepInfo.path) : stepInfo.path != null) {
			return false;
		}
		if (responseType != null ? !responseType.equals(stepInfo.responseType) : stepInfo.responseType != null) {
			return false;
		}
		if (startTime != null ? !startTime.equals(stepInfo.startTime) : stepInfo.startTime != null) {
			return false;
		}
		if (stepId != null ? !stepId.equals(stepInfo.stepId) : stepInfo.stepId != null) {
			return false;
		}
		if (stepName != null ? !stepName.equals(stepInfo.stepName) : stepInfo.stepName != null) {
			return false;
		}
		if (type != null ? !type.equals(stepInfo.type) : stepInfo.type != null) {
			return false;
		}
		if (updatedTime != null ? !updatedTime.equals(stepInfo.updatedTime) : stepInfo.updatedTime != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = stepId != null ? stepId.hashCode() : 0;
		result = 31 * result + (stepName != null ? stepName.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		result = 31 * result + (responseType != null ? responseType.hashCode() : 0);
		result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
		result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
		result = 31 * result + (paused ? 1 : 0);
		result = 31 * result + (orderNumber != null ? orderNumber.hashCode() : 0);
		result = 31 * result + (flowName != null ? flowName.hashCode() : 0);
		result = 31 * result + (flowId != null ? flowId.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (updatedTime != null ? updatedTime.hashCode() : 0);
		return result;
	}
}
