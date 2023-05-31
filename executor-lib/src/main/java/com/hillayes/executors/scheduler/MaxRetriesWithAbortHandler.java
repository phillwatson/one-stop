package com.hillayes.executors.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxRetriesWithAbortHandler<T> implements FailureHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MaxRetriesFailureHandler.class);
    private final int maxRetries;
    private final FailureHandler<T> failureHandler;

    public MaxRetriesWithAbortHandler(int maxRetries, FailureHandler<T> failureHandler) {
        this.maxRetries = maxRetries;
        this.failureHandler = failureHandler;
    }

    public void onFailure(ExecutionComplete executionComplete, ExecutionOperations<T> executionOperations) {
        int consecutiveFailures = executionComplete.getExecution().consecutiveFailures;
        int totalNumberOfFailures = consecutiveFailures + 1;
        if (totalNumberOfFailures > this.maxRetries) {
            LOG.error("Execution has failed {} times for task instance {}. Cancelling execution.", totalNumberOfFailures, executionComplete.getExecution().taskInstance);
            executionOperations.stop();
        } else {
            this.failureHandler.onFailure(executionComplete, executionOperations);
        }
    }
}
