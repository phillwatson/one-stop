package com.hillayes.executors.scheduler.tasks;

import java.util.function.Consumer;

/**
 * A named instance of a named task that can execute jobs on an arbitrary frequency.
 *
 * @see com.hillayes.executors.scheduler.SchedulerFactory#addJob(String, Object)
 */
public interface NamedJobbingTask<T> extends Consumer<T>, NamedTask {
    public Class<T> getDataClass();
}
