package com.hillayes.executors.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.hillayes.executors.correlation.Correlation;
import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import com.hillayes.executors.scheduler.config.RetryConfig;
import com.hillayes.executors.scheduler.config.SchedulerConfig;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.executors.scheduler.tasks.NamedTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.round;

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

        scheduler = scheduleTasks(configuration, jobbingTasks.values(), recurringTasks);
        if (scheduler != null) {
            // inform all tasks that they have been started
            namedTasks.forEach(task -> task.taskInitialised(this));
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
     * @param jobbingTaskName the name of the Jobbing task to pass the payload to for processing.
     * @param payload the payload to be processed.
     * @return the unique identifier for the scheduled job.
     */
    public String addJob(String jobbingTaskName, Object payload) {
        Task<JobbingTaskData> task = jobbingTasks.get(jobbingTaskName);
        if (task == null) {
            throw new IllegalArgumentException("No Jobbing Task found named \"" + jobbingTaskName + "\"");
        }

        String id = UUID.randomUUID().toString();
        log.debug("Scheduling job [name: {}, id: {}]", task.getName(), id);

        // schedule the job's payload - with the caller's correlation ID
        String correlationId = Correlation.getCorrelationId().orElse(id);
        scheduler.schedule(task.instance(id, new JobbingTaskData(correlationId, payload)), Instant.now());

        return id;
    }

    /**
     * Schedules a job of work for the Jobbing task, to process the given payload.
     *
     * @param jobbingTask the Jobbing task to pass the payload to for processing.
     * @param payload the payload to be processed.
     * @return the unique identifier for the scheduled job.
     */
    public String addJob(NamedJobbingTask<?> jobbingTask, Object payload) {
        return addJob(jobbingTask.getName(), payload);
    }

    /**
     * Configures the recurring tasks from the given collection of NamedTasks. Only
     * those tasks for which a configuration is given will be created.
     *
     * @param namedTasks the collection of NamedTasks to be configured.
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

                    // add task - with call-back to execute
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
     * @param namedTasks the collection of NamedTasks to be configured.
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
            Optional<NamedTaskConfig> taskConfig = Optional.ofNullable(configuration.tasks().get(task.getName()));
            if (task instanceof NamedJobbingTask<?>) {
                log.debug("Adding Jobbing Task [name: {}]", task.getName());

                // create a custom task
                Tasks.TaskBuilder<JobbingTaskData> builder = Tasks.custom(task.getName(), JobbingTaskData.class);

                // find any, optional, configuration related to the named task
                taskConfig.ifPresent(config -> {
                    log.debug("Jobbing Task configuration found [task: {}]", task.getName());

                    // use the configured failure handler
                    Optional<FailureHandler<JobbingTaskData>> handler = configureFailureHandler(config);
                    handler.ifPresent(builder::onFailure);
                });

                // add task - with call-back to execute
                result.put(task.getName(), builder.execute((inst, ctx) -> {
                    // construct the context for the task to run in - including the job payload
                    TaskContext<Object> taskContext = new TaskContext<>(
                        inst.getData().getPayloadContent(),
                        ctx.getExecution().consecutiveFailures,
                        inst.getData().repeatCount);

                    // call the task using the correlation ID used when job was queued
                    final NamedJobbingTask<Object> function = (NamedJobbingTask<Object>) task;
                    TaskConclusion conclusion = Correlation.call(inst.getData().correlationId, function, taskContext);

                    // if task has completed
                    if (conclusion == TaskConclusion.COMPLETE) {
                        log.debug("Task completed [instance: {}, repeatCount: {}]",
                            inst.getTaskAndInstance(), taskContext.getRepeatCount());
                        return new CompletionHandler.OnCompleteRemove<>();
                    }

                    // reset the consecutive failure count - as it didn't fail this time
                    ctx.getExecution().consecutiveFailures = 0;

                    // increment the repeat count
                    inst.getData().repeatCount++;

                    // determine when, and if, the incomplete task should be repeated
                    Optional<Duration> repeatInterval = calcRepeatInterval(taskConfig, inst.getData().repeatCount);
                    if (repeatInterval.isPresent()) {
                        log.debug("Task not complete, rescheduling [instance: {}, repeatCount: {}, interval: {}]",
                            inst.getTaskAndInstance(), taskContext.getRepeatCount(), repeatInterval.get());
                        return new CompletionHandler.OnCompleteReschedule<>(
                            Schedules.fixedDelay(repeatInterval.get()), inst.getData());
                    }

                    // max-repeats reached - fail the task
                    log.error("{} has not completed after {} runs. Cancelling execution.",
                        ctx.getExecution().taskInstance, inst.getData().repeatCount);

                    // if an on-max-retry task was named in the config
                    taskConfig
                        .flatMap(NamedTaskConfig::onIncomplete)
                        .flatMap(RetryConfig::onMaxRetry)
                        .ifPresent(onMaxRetryTaskName -> {
                            try {
                                log.debug("Queuing on-max-retry task [name: {}]", onMaxRetryTaskName);
                                Correlation.run(inst.getData().correlationId, () ->
                                    addJob(onMaxRetryTaskName, inst.getData().getPayloadContent())
                                );
                            } catch (Exception e) {
                                log.error("Failed to queue on-max-retry task", e);
                            }
                        });

                    // stop and remove this task instance
                    return new CompletionHandler.OnCompleteRemove<>();
                }));
            }
        });

        return result;
    }

    /**
     * Creates a new Scheduler of the given configuration. The given collections of
     * jobbing and recurring tasks are started.
     *
     * @param configuration the scheduler configuration.
     * @param jobbingTasks the collection of Jobbing tasks to be started.
     * @param recurringTasks the collection of recurring tasks to be started.
     * @return the new scheduler. Will be null if no tasks are given.
     */
    private Scheduler scheduleTasks(SchedulerConfig configuration,
                                    Collection<Task<JobbingTaskData>> jobbingTasks,
                                    Collection<RecurringTask<?>> recurringTasks) {
        if ((jobbingTasks.isEmpty()) && (recurringTasks.isEmpty())) {
            log.info("No scheduler tasks to configure.");
            return null;
        }

        String tableName = configuration.schema()
            .map(schema -> schema + "." + "scheduled_tasks")
            .orElse("scheduled_tasks");

        log.info("Scheduling named scheduled tasks [tableName: {}, jobbingSize: {}, recurringSize: {}]",
            tableName, jobbingTasks.size(), recurringTasks.size());

        Scheduler result = Scheduler.create(dataSource, new ArrayList<>(jobbingTasks))
            .tableName(tableName)
            .serializer(new TaskDataSerializer())
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

    /**
     * Parses the FrequencyConfiguration for a NamedScheduledTask to produce a Schedule
     * on which the task is to be run.
     *
     * @param taskName the name of the NamedScheduledTask to be scheduled.
     * @param config the task's configuration properties.
     * @return the schedule on which the task is run.
     */
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

        return result
            .orElseThrow(() -> new IllegalArgumentException("Schedule period is null - taskName: " + taskName));
    }

    /**
     * Constructs a FailureHandler from the given NamedTaskConfig. The handler may consist of
     * composite handlers.
     *
     * @param namedTaskConfig the configuration from which to create the handler.
     * @return the configured FailureHandler.
     */
    private <T> Optional<FailureHandler<T>> configureFailureHandler(NamedTaskConfig namedTaskConfig) {
        return namedTaskConfig.onFailure().map(retryConfig -> {
            FailureHandler<T> result = null;
            // if a retry config is given
            if (retryConfig.retryInterval().isPresent()) {
                // create a back-off failure handler
                result = new FailureHandler.ExponentialBackoffFailureHandler<>(
                    retryConfig.retryInterval().get(),
                    retryConfig.retryExponent().orElse(RetryConfig.DEFAULT_RETRY_EXPONENT));
            }

            // if a max-retry config is given
            if (retryConfig.maxRetry().isPresent()) {
                // if no retry config was given, use a default
                if (result == null) {
                    result = new FailureHandler.OnFailureRetryLater<>(RetryConfig.DEFAULT_RETRY_INTERVAL);
                }

                // create max-retry failure handler - wrapping the retry handler
                result = new MaxRetriesWithAbortHandler<>(
                    retryConfig.maxRetry().getAsInt(), retryConfig.onMaxRetry().orElse(null), result);
            }
            return result;
        });
    }

    /**
     * Calculates the delay before an INCOMPLETE NamedJobbingTask will be repeated.
     * If no repeat configuration is given, or the max-retries has been reached,
     * the return value will be empty.
     *
     * @param config the NamedJobbingTask's configuration.
     * @param repeatCount the count of the task's repeats
     * @return the delay after which the task will be run.
     */
    private Optional<Duration> calcRepeatInterval(Optional<NamedTaskConfig> config, final int repeatCount) {
        return config
            .flatMap(NamedTaskConfig::onIncomplete)
            .map(c -> {
                if ((c.maxRetry().isPresent()) && (repeatCount > c.maxRetry().getAsInt())) {
                    // indicate it will not be repeated
                    return null;
                }

                long interval = c.retryInterval()
                    .map(Duration::toMillis)
                    .orElse(RetryConfig.DEFAULT_RETRY_INTERVAL.toMillis());

                double exponent = c.retryExponent().orElse(RetryConfig.DEFAULT_RETRY_EXPONENT);

                return Duration.ofMillis(c.retryExponent().isPresent()
                    ? round(interval * pow(repeatCount, exponent))
                    : interval);
            });
    }

    /**
     * Augments a given FailureHandler to add the queuing of another Jobbing Task
     * when the maximum number of retries is exceeded.
     * @param <T>
     */
    public class MaxRetriesWithAbortHandler<T> implements FailureHandler<T> {
        private final int maxRetries;
        private final String onMaxRetryTaskName;
        private final FailureHandler<T> failureHandler;

        public MaxRetriesWithAbortHandler(int maxRetries,
                                          String onMaxRetryTaskName,
                                          FailureHandler<T> failureHandler) {
            this.maxRetries = maxRetries;
            this.onMaxRetryTaskName = onMaxRetryTaskName;
            this.failureHandler = failureHandler;
        }

        public void onFailure(ExecutionComplete executionComplete, ExecutionOperations<T> executionOperations) {
            int consecutiveFailures = executionComplete.getExecution().consecutiveFailures;
            int totalNumberOfFailures = consecutiveFailures + 1;
            if (totalNumberOfFailures > this.maxRetries) {
                log.error("Execution has failed {} times for task instance {}. Cancelling execution.", totalNumberOfFailures, executionComplete.getExecution().taskInstance);
                executionOperations.stop();

                // schedule the abort task
                if (onMaxRetryTaskName != null) {
                    try {
                        log.debug("Queuing on-max-retry task [name: {}]", onMaxRetryTaskName);
                        JobbingTaskData taskData = (JobbingTaskData) executionComplete.getExecution().taskInstance.getData();
                        Correlation.run(taskData.correlationId, () ->
                            SchedulerFactory.this.addJob(onMaxRetryTaskName, taskData.getPayloadContent())
                        );
                    } catch (Exception e) {
                        log.error("Failed to queue on-max-retry task", e);
                    }
                }
            } else {
                this.failureHandler.onFailure(executionComplete, executionOperations);
            }
        }
    }
}
