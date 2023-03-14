package com.hillayes.executors.scheduler.config;

import java.time.Duration;
import java.util.Optional;

/**
 * Configures the frequency at which a NamedTask is scheduled.
 * <p>
 * Only one of the properties should be used. The order in which the values
 * are selected is: recurs(), timeOfDay(), cron().
 *
 */
public interface FrequencyConfig {
    /**
     * Specifies the time between each instance of the NamedTask. If not null,
     * this must by in the ISO-8601 form accepted by java.time.Duration.parse().
     */
    Optional<Duration> recurs();

    /**
     * Specifies the time at which a daily NamedTask is run. If not null,
     * this must be in the ISO-Time format (e.g. 15:10:45).
     */
    Optional<String> timeOfDay();

    /**
     * Specifies a CRON expression to determine the frequency at which a
     * NamedTask is run. If not null, it must comply with the Spring CRON
     * expression format.
     * See https://spring.io/blog/2020/11/10/new-in-spring-5-3-improved-cron-expressions
     */
    Optional<String> cron();
}
