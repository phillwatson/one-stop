package com.hillayes.rail.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.events.consent.ConsentGiven;
import com.hillayes.executors.correlation.Correlation;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.model.AccountDetail;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.scheduled.PollAccountSchedulerTask;
import com.hillayes.rail.service.RailAccountService;
import com.hillayes.rail.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ConsentTopicConsumer {
    private final UserConsentRepository userConsentRepository;
    private final AccountRepository accountRepository;
    private final RequisitionService requisitionService;
    private final RailAccountService railAccountService;
    private final PollAccountSchedulerTask pollAccountSchedulerTask;

    @Incoming("consent")
    @Transactional
    public void consume(EventPacket eventPacket) {
        Correlation.setCorrelationId(eventPacket.getCorrelationId());
        try {
            String payloadClass = eventPacket.getPayloadClass();
            log.debug("Received consent event [payloadClass: {}]", payloadClass);

            if (ConsentGiven.class.getName().equals(payloadClass)) {
                processConsentGiven(eventPacket.getPayloadContent());
            }
        } finally {
            Correlation.setCorrelationId(null);
        }
    }

    private void processConsentGiven(ConsentGiven event) {
        log.info("Processing consent given [userId: {}, consentId: {}]", event.getUserId(), event.getConsentId());
        UserConsent userConsent = userConsentRepository.findById(event.getConsentId()).orElse(null);
        if (userConsent == null) {
            log.debug("User Consent record no longer exists [userId: {}, consentId: {}]", event.getUserId(), event.getConsentId());
            return;
        }

        requisitionService.get(userConsent.getRequisitionId()).ifPresent(requisition ->
            requisition.accounts.forEach(railAccountId -> {
                // locate account by rail-account-id - or create a new one
                Account account = accountRepository.findByRailAccountId(railAccountId)
                    .orElse(Account.builder()
                        .userConsentId(userConsent.getId())
                        .railAccountId(railAccountId)
                        .institutionId(userConsent.getInstitutionId())
                        .build());

                // retrieve rail-account summary
                AccountDetail accountDetail = railAccountService.get(railAccountId);
                account.setOwnerName(accountDetail.ownerName);
                account.setIban(accountDetail.iban);

                // retrieve rail-account details
                Map<String, Object> details = railAccountService.details(railAccountId);
                Map<String, Object> accountProperties = (Map) details.get("account");
                if (accountProperties != null) {
                    account.setAccountName((String) accountProperties.get("name"));
                    account.setAccountType((String) accountProperties.get("cashAccountType"));
                    account.setCurrencyCode((String) accountProperties.get("currency"));
                }

                // save updated account
                account = accountRepository.save(account);

                // schedule the polling of account transactions
                pollAccountSchedulerTask.queueJob(account.getId());
            })
        );
    }
}
