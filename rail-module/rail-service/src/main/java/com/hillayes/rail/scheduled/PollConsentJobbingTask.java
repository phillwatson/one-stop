package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.AgreementStatus;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.UserConsentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * A jobbing task to verify the status of an identified UserConsent.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollConsentJobbingTask extends AbstractNamedJobbingTask<UUID> {
    private final UserConsentService userConsentService;
    private final RailProviderFactory railProviderFactory;
    private final PollAccountJobbingTask pollAccountJobbingTask;

    @Override
    public String getName() {
        return "poll-consent";
    }

    /**
     * @param context the context containing the identifier of the UserConsent to be updated.
     */
    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<UUID> context) {
        UUID consentId = context.getPayload();
        log.info("Processing Poll Consent job [consentId: {}]", consentId);
        UserConsent userConsent = userConsentService.getUserConsent(consentId).orElse(null);
        if (userConsent == null) {
            log.info("Unable to find consent [consentId: {}]", consentId);
            return TaskConclusion.COMPLETE;
        }

        if (userConsent.getStatus() != ConsentStatus.GIVEN) {
            log.info("User consent is no longer GIVEN [consentId: {}, status: {}]", consentId, userConsent.getStatus());
            return TaskConclusion.COMPLETE;
        }

        RailProviderApi railProviderApi = railProviderFactory.get(userConsent.getProvider());
        return railProviderApi.getAgreement(userConsent.getAgreementId())
            .map(agreement -> {
                log.debug("Agreement status [consentId: {}, status: {}]", consentId, agreement.getStatus());
                if (agreement.getStatus() == AgreementStatus.GIVEN) {
                    log.debug("Queuing tasks for account polling [consentId: {}, accountSize: {}]",
                        consentId, agreement.getAccountIds().size());
                    agreement.getAccountIds().forEach(accountId -> pollAccountJobbingTask.queueJob(userConsent.getId(), accountId));
                }

                else if (agreement.getStatus() == AgreementStatus.SUSPENDED) {
                    userConsentService.consentSuspended(userConsent.getId());
                }

                else if (agreement.getStatus() == AgreementStatus.EXPIRED) {
                    userConsentService.consentExpired(userConsent.getId());
                }

                return TaskConclusion.COMPLETE;
            })

            // try again
            .orElseGet(() -> {
                log.info("User consent not ready for polling [consentId: {}, status: {}]", consentId, userConsent.getStatus());
                return TaskConclusion.INCOMPLETE;
            });
    }
}
