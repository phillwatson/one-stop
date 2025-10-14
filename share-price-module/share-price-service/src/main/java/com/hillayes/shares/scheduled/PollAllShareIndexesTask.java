package com.hillayes.shares.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.shares.repository.ShareIndexRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A scheduled task to retrieve the latest share prices for the configured
 * ShareIndex records.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollAllShareIndexesTask implements NamedScheduledTask {
    private final ShareIndexRepository shareIndexRepository;
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask;

    @Override
    public String getName() {
        return "poll-all-share-indexes";
    }

    @Override
    public void taskInitialised(SchedulerFactory scheduler) {
        log.info("PollAllShareIndexesTask.taskScheduled()");
    }

    @Override
    @Transactional
    public void run() {
        log.info("PollAllShareIndexesTask.run()");
        shareIndexRepository.listAll().stream()
            .forEach(shareIndex -> pollShareIndexAdhocTask.queueTask(shareIndex.getId()));
    }
}
