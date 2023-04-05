package com.hillayes.rail.event;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentGiven;
import com.hillayes.events.events.consent.ConsentCancelled;
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

    public void sendConsentGiven(UserConsent userConsent) {
        log.debug("Sending ConsentGiven event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentGiven.builder()
            .consentId(userConsent.getId())
            .dateGiven(userConsent.getDateGiven())
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
            .dateDenied(userConsent.getDateDenied())
            .userId(userConsent.getId())
            .institutionId(userConsent.getInstitutionId())
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .requisitionId(userConsent.getRequisitionId())
            .build());
    }

    public void sendConsentCancelled(UserConsent userConsent) {
        log.debug("Sending ConsentCancelled event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentCancelled.builder()
            .consentId(userConsent.getId())
            .dateCancelled(userConsent.getDateCancelled())
            .userId(userConsent.getId())
            .institutionId(userConsent.getInstitutionId())
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .requisitionId(userConsent.getRequisitionId())
            .build());
    }
}
