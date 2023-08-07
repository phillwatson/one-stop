package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.repository.UserConsentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A scheduled task to verify the status of all User Consent records whose status
 * is currently "GIVEN". To share the load, it will queue a POLL_CONSENT jobbing
 * task for each consent record it finds.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollAllConsentsScheduledTask implements NamedScheduledTask {
    private final UserConsentRepository userConsentRepository;

    private final PollConsentJobbingTask pollConsentJobbingTask;

    @Override
    public String getName() {
        return "poll-all-consents";
    }

    @Override
    public void taskScheduled(SchedulerFactory scheduler) {
        log.info("PollAllConsentsScheduledTask.taskScheduled()");
    }

    @Override
    @Transactional
    public void run() {
        log.info("PollAllConsentsScheduledTask.run()");
        userConsentRepository.listAll().stream()
            .filter(consent -> consent.getStatus() == ConsentStatus.GIVEN)
            .forEach(consent -> pollConsentJobbingTask.queueJob(consent.getId()));
    }
}
