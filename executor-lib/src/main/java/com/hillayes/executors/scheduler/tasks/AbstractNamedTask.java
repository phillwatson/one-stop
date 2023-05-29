package com.hillayes.executors.scheduler.tasks;

import com.hillayes.executors.scheduler.SchedulerFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * A named instance of a task.
 */
@Slf4j
public abstract class AbstractNamedTask implements NamedTask {
    protected final String name;
    protected SchedulerFactory scheduler;

    public AbstractNamedTask(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void taskScheduled(SchedulerFactory scheduler) {
        log.info("{} taskScheduled()", getName());
        this.scheduler = scheduler;
    }
}
