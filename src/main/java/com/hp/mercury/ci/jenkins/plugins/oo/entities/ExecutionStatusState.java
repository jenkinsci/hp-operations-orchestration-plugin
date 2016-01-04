package com.hp.mercury.ci.jenkins.plugins.oo.entities;

/**
 * Created with IntelliJ IDEA.
 * User: levinger
 */

public enum ExecutionStatusState {
    RUNNING,  COMPLETED,  SYSTEM_FAILURE,  PAUSED,  PENDING_PAUSE,  CANCELED,  PENDING_CANCEL,
    NOT_STARTED /*todo:verify we need it*/, RUNNING_BRANCH_PAUSED/*todo: was it removed?*/;

    public static String[] names() {
        ExecutionStatusState[] executionStatusStates = values();
        String[] names = new String[executionStatusStates.length];
        for (int i = 0; i < executionStatusStates.length; i++) {
            names[i] = executionStatusStates[i].name();
        }
        return names;
    }

}
