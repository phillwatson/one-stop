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
 * {@link UserConsentService#registrationTimeout(UUID, String)} method to check if the
 * registration is still pending and if so, mark it as timed out.
 */
@ApplicationScoped
@Slf4j
public class ConsentTimeoutJobbingTask extends AbstractNamedJobbingTask<ConsentTimeoutJobbingTask.Payload> {
    private final UserConsentService userConsentService;

    public ConsentTimeoutJobbingTask(UserConsentService userConsentService) {
        super("consent-timeout");
        this.userConsentService = userConsentService;
    }

    public String queueJob(UserConsent userConsent, Duration timeout) {
        log.info("Queuing job [consentId: {}]", userConsent.getId());
        return scheduler.addJob(this,
            new Payload(userConsent.getId(), userConsent.getReference()),
            Instant.now().plus(timeout));
    }

    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<Payload> context) {
        Payload payload = context.getPayload();
        userConsentService.registrationTimeout(payload.consentId, payload.reference);
        return TaskConclusion.COMPLETE;
    }

    /**
     * The task's payload. Identifies the consent that was initiated. It also carries
     * the unique reference that was assigned to the consent when it was initiated. If
     * the consent fails before it times-out and the user retries the same consent, a
     * new reference would be assigned to that consent. So, the timeout will only be
     * performed if the identified consent carries the same reference.
     *
     * @param consentId the identity of the consent that was initiated.
     * @param reference the unique reference assigned to that consent when it was initiated.
     */
    public record Payload(UUID consentId, String reference) {}
}
