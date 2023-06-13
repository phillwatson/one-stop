package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.rail.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollAllAccountsSchedulerTask implements NamedScheduledTask {
    private final AccountRepository accountRepository;

    private final PollAccountJobbingTask pollAccountJobbingTask;

    @Override
    public String getName() {
        return "poll-all-accounts";
    }

    @Override
    public void taskScheduled(SchedulerFactory scheduler) {
        log.info("PollAllAccountsSchedulerTask.taskScheduled()");
    }

    @Override
    @Transactional
    public void run() {
        log.info("PollAllAccountsSchedulerTask.run()");
        accountRepository.findAll().forEach(account -> {
            pollAccountJobbingTask.queueJob(account.getId());
        });
    }
}
