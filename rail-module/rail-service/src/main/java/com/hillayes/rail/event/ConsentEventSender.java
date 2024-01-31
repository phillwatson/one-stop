package com.hillayes.rail.event;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.*;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ConsentEventSender {
    private final EventSender eventSender;
    private final InstitutionService institutionService;

    public void sendConsentInitiated(UserConsent userConsent) {
        log.debug("Sending ConsentInitiated event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentInitiated.builder()
            .consentId(userConsent.getId())
            .dateInitiated(Instant.now())
            .userId(userConsent.getUserId())
            .institutionId(userConsent.getInstitutionId())
            .institutionName(getInstitutionName(userConsent))
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .build());
    }

    public void sendConsentGiven(UserConsent userConsent) {
        log.debug("Sending ConsentGiven event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentGiven.builder()
            .consentId(userConsent.getId())
            .dateGiven(userConsent.getDateGiven())
            .userId(userConsent.getUserId())
            .institutionId(userConsent.getInstitutionId())
            .institutionName(getInstitutionName(userConsent))
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .build());
    }

    public void sendConsentDenied(UserConsent userConsent) {
        log.debug("Sending ConsentDenied event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentDenied.builder()
            .consentId(userConsent.getId())
            .dateDenied(userConsent.getDateDenied())
            .userId(userConsent.getUserId())
            .institutionId(userConsent.getInstitutionId())
            .institutionName(getInstitutionName(userConsent))
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .errorCode(userConsent.getErrorCode())
            .errorDetail(userConsent.getErrorDetail())
            .build());
    }

    public void sendConsentCancelled(UserConsent userConsent) {
        log.debug("Sending ConsentCancelled event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentCancelled.builder()
            .consentId(userConsent.getId())
            .dateCancelled(userConsent.getDateCancelled())
            .userId(userConsent.getUserId())
            .institutionId(userConsent.getInstitutionId())
            .institutionName(getInstitutionName(userConsent))
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .build());
    }

    public void sendConsentSuspended(UserConsent userConsent) {
        log.debug("Sending ConsentSuspended event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentSuspended.builder()
            .consentId(userConsent.getId())
            .dateSuspended(Instant.now())
            .userId(userConsent.getUserId())
            .institutionId(userConsent.getInstitutionId())
            .institutionName(getInstitutionName(userConsent))
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .build());
    }

    public void sendConsentExpired(UserConsent userConsent) {
        log.debug("Sending ConsentExpired event [consentId: {}, userId: {}, institutionId: {}]",
            userConsent.getId(), userConsent.getUserId(), userConsent.getInstitutionId());
        eventSender.send(Topic.CONSENT, ConsentExpired.builder()
            .consentId(userConsent.getId())
            .dateExpired(Instant.now())
            .userId(userConsent.getUserId())
            .institutionId(userConsent.getInstitutionId())
            .institutionName(getInstitutionName(userConsent))
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .build());
    }

    private String getInstitutionName(UserConsent userConsent) {
        return institutionService.get(userConsent.getProvider(), userConsent.getInstitutionId())
            .map(RailInstitution::getName)
            .orElse("Unknown");
    }
}
