package com.hillayes.executors.scheduler.tasks;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public abstract class AbstractNamedAdhocTask<T>
    extends AbstractNamedTask implements NamedAdhocTask<T> {
    public AbstractNamedAdhocTask() {
        this("");
    }

    public AbstractNamedAdhocTask(String name) {
        super(name);
    }

    /**
     * Queues an instance of this task, with the given payload, for processing.
     *
     * @param payload the data to be processed.
     * @return the identifier the queued instance.
     */
    @Override
    public String queueTask(T payload) {
        log.info("Queuing {} task [payload: {}]", getName(), payload);
        return scheduler.addJob(this, payload);
    }

    public String queueTask(T payload, Instant when) {
        log.info("Queuing {} task [payload: {}, when: {}]", getName(), payload, when.toString());
        return scheduler.addJob(this, payload, when);
    }
}
