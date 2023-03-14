package com.hillayes.executors.scheduler.tasks;

/**
 * A named instance of a task.
 */
public interface NamedTask {
    default public String getName() {
        return this.getClass().getSimpleName();
    }
}
