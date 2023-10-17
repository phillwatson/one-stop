package com.hillayes.executors.scheduler.tasks;

import com.hillayes.executors.scheduler.SchedulerFactory;

/**
 * A named instance of a task.
 */
public interface NamedTask {
    default public String getName() {
        return this.getClass().getSimpleName();
    }

    default public void taskInitialised(SchedulerFactory scheduler) {
    }
}
