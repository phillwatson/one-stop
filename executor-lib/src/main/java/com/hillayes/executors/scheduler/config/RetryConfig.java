package com.hillayes.executors.scheduler.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public interface RetryConfig {
    double DEFAULT_RETRY_EXPONENT = 1.5;
    Duration DEFAULT_RETRY_INTERVAL = Duration.ofMinutes(5);

    /**
     * The initial interval before retrying a task. If required, a value of 1 minute is
     * suggested.
     * <p>
     * Note: the scheduler's polling interval will affect the accuracy at which this
     * interval will be applied.
     * <p>
     * By default, Recurring tasks are rescheduled according to their Schedule one-time
     * tasks are retried again in 5 minutes.
     * @see #retryExponent
     * @see SchedulerConfig#pollingInterval()
     */
    Optional<Duration> retryInterval();

    /**
     * The exponential rate at which a task will be retried. The retryInterval will be
     * increased by this factor on each retry. This will only apply if retryInterval is
     * specified.
     * If retryInterval is specified, a default value of 1.5 is applies.
     */
    OptionalDouble retryExponent();

    /**
     * The maximum number of times a task will be retried. If required, a value of 5 is
     * suggested.
     */
    OptionalInt maxRetry();
}
