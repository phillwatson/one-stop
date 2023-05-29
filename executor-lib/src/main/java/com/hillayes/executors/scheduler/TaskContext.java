package com.hillayes.executors.scheduler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A context passed to the Jobbing task
 */
@RequiredArgsConstructor
@Getter
public class TaskContext<T> {
    /**
     * The payload to be processed; passed when a task was queued.
     */
    private final T payload;

    /**
     * The number of consecutive retries due to error condition. For repeating
     * tasks this will be reset whenever a run passes without failure.
     */
    private int failureCount;

    /**
     * The number of times a repeating task has been executed without completion.
     */
    private int repeatCount;

    public TaskContext<T> setFailureCount(int value) {
        failureCount = value;
        return this;
    }

    public TaskContext<T> setRepeatCount(int value) {
        repeatCount = value;
        return this;
    }
}
