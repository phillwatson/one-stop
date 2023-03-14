package com.hillayes.executors.scheduler.config;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Determines how the scheduler, and the NamedTasks, are configured.
 *
 * See the db-scheduler lib documentation for more information:
 * https://github.com/kagkarlsson/db-scheduler#configuration
 */
public interface ScheduleConfig {
    Integer DEFAULT_THREAD_COUNT = Integer.valueOf(10);
    Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(5);
    Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofMinutes(5);
    Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(30);
    Duration DEFAULT_UNRESOLVED_TIMEOUT = Duration.ofDays(14);

    /**
     * The number of threads used to process work.
     */
    Optional<Integer> threadCount();

    /**
     * Determines the rate at which the scheduler will poll for work.
     */
    Optional<Duration> pollingInterval();

    /**
     * How often to update the heartbeat timestamp for running executions.
     */
    Optional<Duration> heartbeatInterval();

    /**
     * How long the scheduler will wait before interrupting executor-service threads.
     */
    Optional<Duration> shutdownMaxWait();

    /**
     * The time after which executions with unknown tasks are automatically deleted.
     */
    Optional<Duration> unresolvedTimeout();

    /**
     * A collection of NamedTask configuration, keyed on the name of the task to
     * which they relate.
     */
    Map<String, NamedTaskConfig> tasks();
}
