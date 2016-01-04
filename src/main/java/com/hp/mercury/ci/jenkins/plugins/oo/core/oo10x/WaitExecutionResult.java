package com.hp.mercury.ci.jenkins.plugins.oo.core.oo10x;

import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionResult;
import com.hp.mercury.ci.jenkins.plugins.oo.entities.ExecutionStatusState;

/**
 * Created by gurarie on 24/08/2015.
 */
public class WaitExecutionResult {

    private boolean timedOut;
    private ExecutionStatusState lastExecutionStatus;
    private ExecutionResult lastExecutionResult;

    private long stepCount;



    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public ExecutionStatusState getLastExecutionStatus() {
        return lastExecutionStatus;
    }

    public void setLastExecutionStatus(ExecutionStatusState lastExecutionStatus) {
        this.lastExecutionStatus = lastExecutionStatus;
    }

    public long getStepCount() {
        return stepCount;
    }

    public void setStepCount(long stepCount) {
        this.stepCount = stepCount;
    }

    public ExecutionResult getLastExecutionResult() {
        return lastExecutionResult;
    }

    public void setLastExecutionResult(ExecutionResult lastExecutionResult) {
        this.lastExecutionResult = lastExecutionResult;
    }

    public String getFullStatusName(){
        if (lastExecutionStatus.equals(ExecutionStatusState.COMPLETED)){
            return lastExecutionStatus + " - " + lastExecutionResult;
        }else{
            //TODO handle also paused runs
            return lastExecutionStatus.name();
        }
    }
}
