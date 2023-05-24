package com.hillayes.executors.scheduler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A context passed to the Jobbing task
 */
@RequiredArgsConstructor
@Getter
public class TaskContext<T> {
    private final int retryCount;
    private final T payload;
}
