package com.hillayes.executors.scheduler.tasks;

import com.hillayes.executors.scheduler.SchedulerFactory;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A named instance of a named task that can execute jobs on an arbitrary frequency.
 *
 * @see com.hillayes.executors.scheduler.SchedulerFactory#addJob(NamedJobbingTask, Serializable)
 */
public interface NamedJobbingTask<T extends Serializable> extends Consumer<T>, NamedTask {
    public String queueJob(T payload);
}
