package com.hillayes.executors.scheduler.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Configures a named task. Each entry is listed within a Map, keyed on the
 * name of the task to which it refers.
 */
public interface NamedTaskConfig {
    double DEFAULT_RETRY_EXPONENT = 1.5;

    /**
     * Determines how a NamedScheduledTask is scheduled. This is not used by
     * NamedJobbingTasks, which process tasks in an ad-hoc manner.
     */
    Optional<FrequencyConfig> frequency();

    /**
     * The initial interval before retrying a failed task. If required, a value of 1 minute
     * is suggested.
     * <p>
     * By default, Recurring tasks are rescheduled according to their Schedule one-time
     * tasks are retried again in 5 minutes.
     * @see #retryExponent
     */
    Optional<Duration> retryInterval();

    /**
     * The exponential rate at which a failed task will be retried. The retryInterval will
     * be increased by this factor on each retry. This will only apply if retryInterval is
     * specified.
     * If required, a value of 1.5 is suggested.
     */
    OptionalDouble retryExponent();
}
