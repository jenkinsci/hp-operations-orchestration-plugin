package com.hp.mercury.ci.jenkins.plugins.oo.entities;


public enum ExecutionResult {
    RESOLVED, SUCCESS, FAILURE, SYSTEM_CANCELLED, SYSTEM_FAILURE, COMPLETED, SUSPEND, FINISHED, RUNNING, NOT_FINISHED, EXCEPTION, ERROR, NO_ACTION_TAKEN, UNKNOWN, DIAGNOSED;

    public static String[] names() {
        ExecutionResult[] executionResults = values();
        String[] names = new String[executionResults.length];
        for (int i = 0; i < executionResults.length; i++) {
            names[i] = executionResults[i].name();
        }
        return names;
    }

}


