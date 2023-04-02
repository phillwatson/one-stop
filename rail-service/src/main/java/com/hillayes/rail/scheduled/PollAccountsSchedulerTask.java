package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class PollAccountsSchedulerTask implements NamedScheduledTask {
    @Override
    public String getName() {
        return "poll-accountDetails";
    }

    @Override
    public void taskScheduled(SchedulerFactory scheduler) {
        log.info("PollAccountsSchedulerTask.taskScheduled()");
    }

    @Override
    public void run() {
        log.info("PollAccountsSchedulerTask.run()");
    }
}
