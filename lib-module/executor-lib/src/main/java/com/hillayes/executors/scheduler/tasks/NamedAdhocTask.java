package com.hillayes.executors.scheduler.tasks;

import com.hillayes.executors.scheduler.TaskContext;

import java.util.function.Function;

public interface NamedAdhocTask<T>
    extends Function<TaskContext<T>, TaskConclusion>, NamedTask {

    /**
     * Queues an instance of this task, with the given payload, for processing.
     *
     * @param payload the data to be processed.
     * @return the identifier of the queued task instance.
     */
    public String queueTask(T payload);
}
