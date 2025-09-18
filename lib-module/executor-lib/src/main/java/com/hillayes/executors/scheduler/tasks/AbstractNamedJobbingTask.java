package com.hillayes.executors.scheduler.tasks;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public abstract class AbstractNamedJobbingTask<T>
    extends AbstractNamedTask implements NamedJobbingTask<T> {
    public AbstractNamedJobbingTask() {
        this("");
    }

    public AbstractNamedJobbingTask(String name) {
        super(name);
    }

    /**
     * Queues an instance of this task, with the given payload, for processing.
     *
     * @param payload the data to be processed.
     * @return the job identifier.
     */
    @Override
    public String queueTask(T payload) {
        log.info("Queuing {} job [payload: {}]", getName(), payload);
        return scheduler.addJob(this, payload);
    }

    public String queueTask(T payload, Instant when) {
        log.info("Queuing {} job [payload: {}, when: {}]", getName(), payload, when.toString());
        return scheduler.addJob(this, payload, when);
    }
}
