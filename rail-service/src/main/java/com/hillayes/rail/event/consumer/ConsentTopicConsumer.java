package com.hillayes.rail.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentGiven;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.scheduled.PollAccountJobbingTask;
import com.hillayes.rail.service.RailAccountService;
import com.hillayes.rail.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@TopicConsumer(Topic.CONSENT)
@RequiredArgsConstructor
@Slf4j
public class ConsentTopicConsumer implements EventConsumer {
    private final UserConsentRepository userConsentRepository;
    private final AccountRepository accountRepository;
    private final RequisitionService requisitionService;
    private final RailAccountService railAccountService;
    private final PollAccountJobbingTask pollAccountJobbingTask;

    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.debug("Received consent event [payloadClass: {}]", payloadClass);

        if (ConsentGiven.class.getName().equals(payloadClass)) {
            processConsentGiven(eventPacket.getPayloadContent());
        }
    }

    /**
     * Processes ConsentGiven events by retrieving the details of the accounts
     * identified by the rail requisition record. For each account identified in
     * the requisition an Account record will be created and a task will be
     * scheduled to poll the account's transactions.
     *
     * @param event the ConsentGiven event identifying the consent record.
     */
    private boolean processConsentGiven(ConsentGiven event) {
        log.info("Processing consent given [userId: {}, consentId: {}]", event.getUserId(), event.getConsentId());

        // read the consent record to ensure it's still active
        UserConsent userConsent = userConsentRepository
            .findById(event.getConsentId())
            .orElse(null);

        if (userConsent == null) {
            log.debug("User Consent record no longer exists [userId: {}, consentId: {}]",
                event.getUserId(), event.getConsentId());
            return true;
        }

        if (userConsent.getStatus() != ConsentStatus.GIVEN) {
            log.debug("User Consent record no longer active [userId: {}, consentId: {}, status: {}]",
                event.getUserId(), event.getConsentId(), userConsent.getStatus());
            return true;
        }

        return requisitionService.get(userConsent.getRequisitionId()).map(requisition -> {
            // TODO: ensure requisition status is "LN"

            // retrieve details of each account in the requisition
            requisition.accounts.forEach(railAccountId -> {
                // retrieve rail-account summary
                railAccountService.get(railAccountId).map(summary -> {
                    // TODO: ensure account status is "READY"

                    // locate account by rail-account-id - or create a new one
                    final Account account = accountRepository.findByRailAccountId(railAccountId)
                        .orElse(Account.builder()
                            .userConsentId(userConsent.getId())
                            .userId(userConsent.getUserId())
                            .institutionId(userConsent.getInstitutionId())
                            .railAccountId(railAccountId)
                            .ownerName(summary.ownerName)
                            .iban(summary.iban)
                            .build());

                    // retrieve rail-account details
                    railAccountService.details(railAccountId).ifPresent(details -> {
                        Map<String, Object> accountProperties = (Map) details.get("account");
                        if (accountProperties != null) {
                            account.setAccountName((String) accountProperties.get("name"));
                            account.setAccountType((String) accountProperties.get("cashAccountType"));
                            account.setCurrencyCode((String) accountProperties.get("currency"));
                        }
                    });

                    // save updated account
                    UUID id = accountRepository.save(account).getId();

                    // schedule the polling of account transactions
                    pollAccountJobbingTask.queueJob(id);
                    return id;
                }).orElseGet(() -> {
                    log.warn("Failed to retrieve account [accountId: {}]", railAccountId);
                    return null;
                });
            });
            return true;
        }).orElseGet(() -> {
            log.warn("Failed to retrieve requisition [requisition: {}]", userConsent.getRequisitionId());
            return false;
        });
    }
}
