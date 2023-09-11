package com.hillayes.executors.scheduler.config;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Determines how the scheduler, and the NamedTasks, are configured.
 *
 * See the db-scheduler lib documentation for more information:
 * https://github.com/kagkarlsson/db-scheduler#configuration
 */
@ConfigMapping(prefix = "one-stop.scheduler")
public interface SchedulerConfig {
    final static Integer DEFAULT_THREAD_COUNT = 10;
    final static Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(5);
    final static Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofMinutes(5);
    final static Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(30);
    final static Duration DEFAULT_UNRESOLVED_TIMEOUT = Duration.ofDays(14);

    /**
     * The name of the DB schema in which the scheduler tables are stored.
     */
    Optional<String> schema();

    /**
     * The number of threads used to process work. The default is 10.
     */
    Optional<Integer> threadCount();

    /**
     * Determines the rate at which the scheduler will poll for work. The default
     * is 5 seconds.
     */
    Optional<Duration> pollingInterval();

    /**
     * How often to update the heartbeat timestamp for running executions. The default
     * is 5 minutes.
     */
    Optional<Duration> heartbeatInterval();

    /**
     * How long the scheduler will wait before interrupting executor-service threads.
     * The default is 30 minutes.
     */
    Optional<Duration> shutdownMaxWait();

    /**
     * The time after which executions with unknown tasks are automatically deleted.
     * The default is 14 days.
     */
    Optional<Duration> unresolvedTimeout();

    /**
     * A collection of NamedTask configuration, keyed on the name of the task to
     * which they relate.
     */
    Map<String, NamedTaskConfig> tasks();
}
