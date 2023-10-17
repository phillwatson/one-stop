package com.hillayes.executors.scheduler.config;

import java.util.Optional;

/**
 * Configures a named task. Each entry is listed within a Map, keyed on the
 * name of the task to which it refers.
 */
public interface NamedTaskConfig {
    /**
     * Determines how a NamedScheduledTask is scheduled. This is not used by
     * NamedJobbingTasks, which process tasks in an ad-hoc manner.
     */
    Optional<FrequencyConfig> frequency();

    /**
     * Determines how a task is to be retried should it fail with an exception.
     */
    Optional<RetryConfig> onFailure();

    /**
     * Determines how a task is to be retried should it return an INCOMPLETE conclusion.
     */
    Optional<RetryConfig> onIncomplete();
}
