package com.hillayes.executors.scheduler;

import lombok.Getter;

/**
 * A context passed to the Jobbing task
 */
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
    private final int failureCount;

    /**
     * The number of times a repeating task has been executed without completion.
     */
    private final int repeatCount;

    public TaskContext(T payload) {
        this(payload, 0, 0);
    }

    public TaskContext(T payload, int failureCount, int repeatCount) {
        this.payload = payload;
        this.failureCount = failureCount;
        this.repeatCount = repeatCount;
    }
}
