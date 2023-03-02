package com.hillayes.rail.events;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentAccepted;
import com.hillayes.events.events.consent.ConsentDenied;
import com.hillayes.events.events.consent.ConsentInitiated;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.rail.domain.UserConsent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ConsentEventSender {
    private final EventSender eventSender;

    public void sendConsentInitiated(UserConsent userConsent) {
        log.debug("Sending ConsentInitiated event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentInitiated.builder()
            .consentId(userConsent.getId())
            .dateInitiated(Instant.now())
            .userId(userConsent.getId())
            .institutionId(userConsent.getInstitutionId())
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .requisitionId(userConsent.getRequisitionId())
            .build());
    }

    public void sendConsentAccepted(UserConsent userConsent) {
        log.debug("Sending ConsentAccepted event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentAccepted.builder()
            .consentId(userConsent.getId())
            .dateAccepted(Instant.now())
            .userId(userConsent.getId())
            .institutionId(userConsent.getInstitutionId())
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .requisitionId(userConsent.getRequisitionId())
            .build());
    }

    public void sendConsentDenied(UserConsent userConsent) {
        log.debug("Sending ConsentDenied event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentDenied.builder()
            .consentId(userConsent.getId())
            .dateDenied(Instant.now())
            .userId(userConsent.getId())
            .institutionId(userConsent.getInstitutionId())
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .requisitionId(userConsent.getRequisitionId())
            .build());
    }
}
