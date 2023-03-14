package com.hillayes.executors.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.hillayes.executors.correlation.Correlation;
import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.SchedulerConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.executors.scheduler.tasks.NamedTask;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
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

    private final SchedulerConfig configuration;

    private final Scheduler scheduler;

    private final Map<String, Task<?>> jobbingTasks;

    public SchedulerFactory(DataSource dataSource,
                            SchedulerConfig configuration,
                            Iterable<NamedTask> namedTasks) {
        this.dataSource = dataSource;
        this.configuration = configuration;

        this.jobbingTasks = createJobbingTasks(namedTasks);
        List<RecurringTask<?>> recurringTasks = createScheduledTasks(namedTasks, configuration);

        log.info("Scheduling named scheduled tasks [jobbingSize: {}, recurringSize: {}]",
            jobbingTasks.size(), recurringTasks.size());
        this.scheduler = schedule(configuration, jobbingTasks.values(), recurringTasks);
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
     * @param name    the name of the Jobbing task to pass the payload to for processing.
     * @param payload the payload to be processed.
     * @return the unique identifier for the scheduled job.
     */
    public String addJob(String name, Object payload) {
        Task<Object> task = (Task<Object>) jobbingTasks.get(name);
        if (task == null) {
            throw new IllegalArgumentException("No Jobbing Task found named \"" + name + "\"");
        }

        String id = UUID.randomUUID().toString();
        log.debug("Scheduling job [name: {}, id: {}]", task.getName(), id);
        scheduler.schedule(task.instance(id, payload), Instant.now());

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
                log.debug("Scheduled Task [name: {}, configured: {}]", task.getName(), namedTaskConfig != null);

                if (namedTaskConfig != null) {
                    Schedule schedule = parseSchedule(task.getName(), namedTaskConfig);
                    log.debug("Adding Scheduled Task [name: {}, schedule: {}]", task.getName(), schedule);
                    result.add(Tasks
                        .recurring(task.getName(), schedule)
                        .execute((inst, ctx) -> Correlation.run(task.getName(), (Runnable) task))
                    );
                }
            }
        });

        return result;
    }

    /**
     * Configures jobbing tasks from the given collection NamedTasks.
     *
     * @param namedTasks the collection of NamedTasks to be configured.
     * @return the configured collection of jobbing tasks.
     */
    private Map<String, Task<?>> createJobbingTasks(Iterable<NamedTask> namedTasks) {
        if (namedTasks == null) {
            return Collections.emptyMap();
        }

        Map<String, Task<?>> result = new HashMap<>();
        namedTasks.forEach(task -> {
            if (task instanceof NamedJobbingTask<?>) {
                log.debug("Adding Jobbing Task [name: {}]", task.getName());
                if (result.containsKey(task.getName())) {
                    throw new IllegalArgumentException("Duplicate Jobbing Task name - " + task.getName());
                }

                // prepare a jobbing task
                NamedJobbingTask<Object> consumer = (NamedJobbingTask<Object>) task;
                Class<?> dataClass = ((NamedJobbingTask<?>) task).getDataClass();

                result.put(task.getName(), Tasks.oneTime(task.getName(), dataClass)
                    .execute((inst, ctx) -> Correlation.call(task.getName(), consumer, inst.getData()))
                );
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
    private Scheduler schedule(SchedulerConfig configuration,
                               Collection<Task<?>> jobbingTasks,
                               Collection<RecurringTask<?>> recurringTasks) {
        if ((jobbingTasks.isEmpty()) && (recurringTasks.isEmpty())) {
            return null;
        }

        log.debug("Creating scheduler");
        Scheduler result = Scheduler.create(dataSource, new ArrayList<>(jobbingTasks))
            .startTasks(new ArrayList<>(recurringTasks))
            .threads(configuration.threadCount().orElse(SchedulerConfig.DEFAULT_THREAD_COUNT))
            .pollingInterval(configuration.pollingInterval().orElse(SchedulerConfig.DEFAULT_POLLING_INTERVAL))
            .heartbeatInterval(configuration.heartbeatInterval().orElse(SchedulerConfig.DEFAULT_HEARTBEAT_INTERVAL))
            .shutdownMaxWait(configuration.shutdownMaxWait().orElse(SchedulerConfig.DEFAULT_SHUTDOWN_MAX_WAIT))
            .deleteUnresolvedAfter(configuration.unresolvedTimeout().orElse(SchedulerConfig.DEFAULT_UNRESOLVED_TIMEOUT))
            .registerShutdownHook()
            .build();

        result.start();
        return result;
    }

    private Schedule parseSchedule(String taskName, NamedTaskConfig config) {
        FrequencyConfig frequencyConfig = config.frequency()
            .orElseThrow(() -> new IllegalArgumentException("Schedule frequency may not be null - taskName: " + taskName));

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
            throw new IllegalArgumentException("Schedule period may not be null - taskName: " + taskName);
        }

        return result.get();
    }
}
