package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.UserConsentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A jobbing task that will be queued when a user consent registration is initiated,
 * and scheduled to run when a user consent registration times out. It will call the
 * {@link UserConsentService#registrationTimeout(UUID)} method to check if the
 * registration is still pending and if so, mark it as timed out.
 */
@ApplicationScoped
@Slf4j
public class ConsentTimeoutJobbingTask extends AbstractNamedJobbingTask<UUID> {
    private final UserConsentService userConsentService;

    public ConsentTimeoutJobbingTask(UserConsentService userConsentService) {
        super("consent-timeout");
        this.userConsentService = userConsentService;
    }

    public String queueJob(UserConsent userConsent, Duration timeout) {
        log.info("Queuing job [consentId: {}]", userConsent.getId());
        return scheduler.addJob(this, userConsent.getId(), Instant.now().plus(timeout));
    }

    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<UUID> context) {
        UUID consentId = context.getPayload();
        userConsentService.registrationTimeout(consentId);
        return TaskConclusion.COMPLETE;
    }
}
