package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class PollAccountSchedulerTask implements NamedJobbingTask<UUID> {
    private SchedulerFactory scheduler;

    @Override
    public String getName() {
        return "poll-account";
    }

    @Override
    public void taskScheduled(SchedulerFactory scheduler) {
        log.info("PollAccountTask.taskScheduled()");
        this.scheduler = scheduler;
    }

    @Override
    public String queueJob(UUID accountId) {
        log.info("Queuing PollAccountTask job [accountId: {}]", accountId);
        return scheduler.addJob(this, accountId);
    }

    @Override
    public void accept(UUID accountId) {
        log.info("Processing PollAccountTask job [accountId: {}]", accountId);
    }
}
