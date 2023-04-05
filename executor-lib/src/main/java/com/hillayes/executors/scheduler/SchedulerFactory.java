package com.hillayes.executors.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.hillayes.executors.correlation.Correlation;
import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import com.hillayes.executors.scheduler.config.SchedulerConfig;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.executors.scheduler.tasks.NamedTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

/**
 * Creates a facade around the kagkarlsson db-scheduler library; with the aim of
 * reducing our dependency on that library. https://github.com/kagkarlsson/db-scheduler
 * <p>
 * The concept behind db-scheduler is; tasks (along with any data) are persisted
 * to the database and shared by all service nodes in a cluster. To enable this,
 * all service nodes must initialise a scheduler (usually on start-up) to poll the
 * database for named tasks. The polled tasks are then passed to worker threads for
 * processing. These worker threads will remain running until the service is shutdown.
 * <p>
 * In this facade, the initialisation of the scheduler is governed by the configuration
 * (see {@link SchedulerConfig}), and a coll
 */
@Slf4j
public class SchedulerFactory {
    private final DataSource dataSource;

    private final Scheduler scheduler;

    private final Map<String, Task<JobbingTaskData>> jobbingTasks;

    public SchedulerFactory(DataSource dataSource,
                            SchedulerConfig configuration,
                            Iterable<NamedTask> namedTasks) {
        log.info("Initialising SchedulerFactory");
        Set<String> names = new HashSet<>();
        namedTasks.forEach(task -> {
            if (names.contains(task.getName())) {
                throw new IllegalArgumentException("Duplicate Task name - " + task.getName());
            }
            names.add(task.getName());
        });

        this.dataSource = dataSource;

        jobbingTasks = createJobbingTasks(namedTasks, configuration);
        List<RecurringTask<?>> recurringTasks = createScheduledTasks(namedTasks, configuration);

        log.info("Scheduling named scheduled tasks [jobbingSize: {}, recurringSize: {}]",
            jobbingTasks.size(), recurringTasks.size());
        scheduler = scheduleTasks(configuration, jobbingTasks.values(), recurringTasks);
        if (scheduler != null) {
            // inform all tasks that they have been started
            namedTasks.forEach(task -> task.taskScheduled(this));
        }
    }

    /**
     * Can be used to stop the polling for work, and to cancel tasks currently
     * running.
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    /**
     * Schedules a job of work for the named Jobbing task, to process the given payload.
     *
     * @param jobbingTask the Jobbing task to pass the payload to for processing.
     * @param payload     the payload to be processed.
     * @return the unique identifier for the scheduled job.
     */
    public String addJob(NamedJobbingTask<?> jobbingTask, Serializable payload) {
        Task<JobbingTaskData> task = jobbingTasks.get(jobbingTask.getName());
        if (task == null) {
            throw new IllegalArgumentException("No Jobbing Task found named \"" + jobbingTask.getName() + "\"");
        }

        String id = UUID.randomUUID().toString();
        log.debug("Scheduling job [name: {}, id: {}]", task.getName(), id);

        // schedule the job's payload - with the caller's correlation ID
        String correlationId = Correlation.getCorrelationId().orElse(id);
        scheduler.schedule(task.instance(id, new JobbingTaskData(correlationId, payload)), Instant.now());

        return id;
    }

    /**
     * Configures the recurring tasks from the given collection of NamedTasks. Only
     * those tasks for which a configuration is given will be created.
     *
     * @param namedTasks    the collection of NamedTasks to be configured.
     * @param configuration the configurations to be matched to the NamedTasks.
     * @return the configured collection of recurring tasks.
     */
    private List<RecurringTask<?>> createScheduledTasks(Iterable<NamedTask> namedTasks,
                                                        SchedulerConfig configuration) {
        if (namedTasks == null) {
            return Collections.emptyList();
        }

        List<RecurringTask<?>> result = new ArrayList<>();
        namedTasks.forEach(task -> {
            if (task instanceof NamedScheduledTask) {
                // find any configuration related to the named task
                NamedTaskConfig namedTaskConfig = configuration.tasks().get(task.getName());
                if (namedTaskConfig == null) {
                    log.debug("No Scheduled Task configuration found [task: {}]", task.getName());
                } else {
                    Schedule schedule = parseSchedule(task.getName(), namedTaskConfig);
                    log.debug("Adding Scheduled Task [name: {}, schedule: {}]", task.getName(), schedule);

                    // create and configure the recurring task
                    Tasks.RecurringTaskBuilder<Void> builder = Tasks.recurring(task.getName(), schedule);

                    Optional<FailureHandler<Void>> handler = configureFailureHandler(namedTaskConfig);
                    handler.ifPresent(builder::onFailure);

                    // add task (with call-back) to the result
                    result.add(builder.execute((inst, ctx) ->
                        // call the task - use the task name as a correlation ID
                        Correlation.run(inst.getTaskAndInstance(), (Runnable) task)
                    ));
                }
            }
        });

        return result;
    }

    /**
     * Configures jobbing tasks from the given collection NamedTasks.
     * <p>
     * The given SchedulerConfig MAY contain configurations for the named jobbing tasks, but
     * defaults will be applied if no config entry is found.
     *
     * @param namedTasks    the collection of NamedTasks to be configured.
     * @param configuration the configurations from which the tasks', optional, config is taken.
     * @return the configured collection of jobbing tasks.
     */
    private Map<String, Task<JobbingTaskData>> createJobbingTasks(Iterable<NamedTask> namedTasks,
                                                                  SchedulerConfig configuration) {
        if (namedTasks == null) {
            return Collections.emptyMap();
        }

        Map<String, Task<JobbingTaskData>> result = new HashMap<>();
        namedTasks.forEach(task -> {
            if (task instanceof NamedJobbingTask<?>) {
                log.debug("Adding Jobbing Task [name: {}]", task.getName());

                // create one-time-task
                Tasks.OneTimeTaskBuilder<JobbingTaskData> builder = Tasks.oneTime(task.getName(), JobbingTaskData.class);

                // find any, optional, configuration related to the named task
                NamedTaskConfig namedTaskConfig = configuration.tasks().get(task.getName());
                if (namedTaskConfig != null) {
                    log.debug("Jobbing Task configuration found [task: {}]", task.getName());

                    // use the configured failure handler
                    Optional<FailureHandler<JobbingTaskData>> handler = configureFailureHandler(namedTaskConfig);
                    handler.ifPresent(builder::onFailure);
                }

                // add task (with call-back) to the result
                final NamedJobbingTask<Serializable> consumer = (NamedJobbingTask<Serializable>) task;
                result.put(task.getName(), builder.execute((inst, ctx) ->
                    // call the task with the payload data - using the correlation ID used when job was queued
                    Correlation.call(inst.getData().correlationId, consumer, inst.getData().payload)
                ));
            }
        });

        return result;
    }

    /**
     * Creates a new Scheduler of the given configuration. The given collections of
     * jobbing and recurring tasks are started.
     *
     * @param configuration  the scheduler configuration.
     * @param jobbingTasks   the collection of Jobbing tasks to be started.
     * @param recurringTasks the collection of recurring tasks to be started.
     * @return the new scheduler. Will be null if no tasks are given.
     */
    private Scheduler scheduleTasks(SchedulerConfig configuration,
                                    Collection<Task<JobbingTaskData>> jobbingTasks,
                                    Collection<RecurringTask<?>> recurringTasks) {
        if ((jobbingTasks.isEmpty()) && (recurringTasks.isEmpty())) {
            return null;
        }

        log.debug("Creating scheduler");
        String tableName = configuration.schema()
            .map(schema -> schema + "." + "scheduled_tasks")
            .orElse("scheduled_tasks");
        Scheduler result = Scheduler.create(dataSource, new ArrayList<>(jobbingTasks))
            .tableName(tableName)
            .threads(configuration.threadCount().orElse(SchedulerConfig.DEFAULT_THREAD_COUNT))
            .pollingInterval(configuration.pollingInterval().orElse(SchedulerConfig.DEFAULT_POLLING_INTERVAL))
            .heartbeatInterval(configuration.heartbeatInterval().orElse(SchedulerConfig.DEFAULT_HEARTBEAT_INTERVAL))
            .shutdownMaxWait(configuration.shutdownMaxWait().orElse(SchedulerConfig.DEFAULT_SHUTDOWN_MAX_WAIT))
            .deleteUnresolvedAfter(configuration.unresolvedTimeout().orElse(SchedulerConfig.DEFAULT_UNRESOLVED_TIMEOUT))
            .startTasks(new ArrayList<>(recurringTasks))
            .registerShutdownHook()
            .build();

        result.start();
        return result;
    }

    private Schedule parseSchedule(String taskName, NamedTaskConfig config) {
        FrequencyConfig frequencyConfig = config.frequency()
            .orElseThrow(() -> new IllegalArgumentException("Schedule frequency is null - taskName: " + taskName));

        Optional<Schedule> result = Optional.empty();
        if (frequencyConfig != null) {
            result = frequencyConfig.recurs()
                .map(Schedules::fixedDelay);

            if (result.isEmpty()) {
                result = frequencyConfig.timeOfDay()
                    .map(LocalTime::parse)
                    .map(Schedules::daily);
            }

            if (result.isEmpty()) {
                result = frequencyConfig.cron()
                    .map(Schedules::cron);
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Schedule period is null - taskName: " + taskName);
        }

        return result.get();
    }

    /**
     * Constructs a FailureHandler from the given NamedTaskConfig. The handler may consist of
     * composite handlers.
     *
     * @param namedTaskConfig the configuration from which to create the handler.
     * @return the configured FailureHandler.
     */
    private <T> Optional<FailureHandler<T>> configureFailureHandler(NamedTaskConfig namedTaskConfig) {
        FailureHandler<T> result = null;

        // if a retry config is given
        if (namedTaskConfig.retryInterval().isPresent()) {
            // create a back-off failure handler
            result = new FailureHandler.ExponentialBackoffFailureHandler<>(
                namedTaskConfig.retryInterval().get(),
                namedTaskConfig.retryExponent().orElse(NamedTaskConfig.DEFAULT_RETRY_EXPONENT));
        }

        // if a max-retry config is given
        if (namedTaskConfig.maxRetry().isPresent()) {
            // if no retry config was given, use a default
            if (result == null) {
                result = new FailureHandler.OnFailureRetryLater<>(Duration.ofMinutes(1));
            }

            // create max-retry failure handler - wrapping the retry handler
            result = new FailureHandler.MaxRetriesFailureHandler<>(
                namedTaskConfig.maxRetry().getAsInt(), result);
        }

        return Optional.ofNullable(result);
    }

    /**
     * A data-class that is persisted with to NamedJobbingTask's queue. It records
     * the payload to be processed by the NamedJobbingTask, and the correlation ID
     * that was active at the time the job was queued. This allows the correlation
     * ID to be re-activated when the task is processed.
     */
    @AllArgsConstructor
    private static class JobbingTaskData implements Serializable {
        String correlationId;
        Serializable payload;
    }
}
