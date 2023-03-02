package com.hillayes.rail.services;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.errors.BankRegistrationException;
import com.hillayes.rail.events.ConsentEventSender;
import com.hillayes.rail.model.*;
import com.hillayes.rail.repository.UserConsentRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class UserConsentService {
    @Inject
    ServiceConfiguration config;
    @Inject
    UserConsentRepository userConsentRepository;
    @Inject
    @RestClient
    InstitutionService institutionService;
    @Inject
    @RestClient
    AgreementService agreementService;
    @Inject
    @RestClient
    RequisitionService requisitionService;
    @Inject
    ConsentEventSender consentEventSender;

    public List<UserConsent> listConsents(UUID userId) {
        log.info("Listing user's banks [userId: {}]", userId);
        List<UserConsent> result = userConsentRepository.findByUserId(userId);
        log.debug("Listing user's banks [userId: {}, size: {}]", userId, result.size());
        return result;
    }

    public URI register(UUID userId, String institutionId) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);
        if (userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId).size() > 0) {
            throw new BankAlreadyRegisteredException(userId, institutionId);
        }

        Institution institution = institutionService.get(institutionId);

        // create agreement
        log.debug("Requesting agreement [userId: {}, institutionId: {}]", userId, institutionId);
        EndUserAgreement agreement = agreementService.create(EndUserAgreementRequest.builder()
            .institutionId(institutionId)
            .accessScope(List.of("balances", "details", "transactions"))
            .maxHistoricalDays(institution.transactionTotalDays)
            .accessValidForDays(90)
            .build());

        // calculate expiry data
        Instant expires = Instant.now()
            .truncatedTo(ChronoUnit.DAYS)
            .plus(agreement.accessValidForDays, ChronoUnit.DAYS);

        // record agreement
        log.debug("Recording agreement [userId: {}, institutionId: {}, expires: {}]", userId, institutionId, expires);
        UserConsent userConsent = UserConsent.builder()
            .userId(userId)
            .institutionId(agreement.institutionId)
            .agreementId(agreement.id.toString())
            .maxHistory(agreement.maxHistoricalDays)
            .agreementExpires(expires)
            .status(ConsentStatus.INITIATED)
            .build();

        // save to generate ID
        userConsent = userConsentRepository.saveAndFlush(userConsent);

        try {
            // create requisition
            log.debug("Requesting requisition [userId: {}, institutionId: {}: ref: {}]", userId, institutionId, userConsent.getId());
            Requisition requisition = requisitionService.create(RequisitionRequest.builder()
                .institutionId(agreement.institutionId)
                .agreement(agreement.id)
                .userLanguage("EN")
                .reference(userConsent.getId().toString())
                .redirect(config.callbackUrl())
                .redirectImmediate(Boolean.FALSE)
                .build());

            // record requisition
            userConsent.setRequisitionId(requisition.id.toString());
            userConsent.setStatus(ConsentStatus.WAITING);

            // send consent initiated event notification
            consentEventSender.sendConsentInitiated(userConsent);

            // return link for user consent
            log.debug("Returning consent link [userId: {}, institutionId: {}, link: {}]", userId, institutionId, requisition.link);
            return URI.create(requisition.link);
        } catch (Exception e) {
            throw new BankRegistrationException(userId, institutionId, e);
        }
    }

    public void consentAccepted(UUID userConsentId) {
        log.info("User's consent received [userConsentId: {}]", userConsentId);
        UserConsent userConsent = userConsentRepository.findById(userConsentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", userConsentId));

        log.debug("Recording consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
            userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
        userConsent.setStatus(ConsentStatus.GIVEN);
        userConsent = userConsentRepository.save(userConsent);

        // send consent accepted event notification
        consentEventSender.sendConsentAccepted(userConsent);
    }

    public void consentDenied(UUID userConsentId, String error, String details) {
        log.info("User's consent denied [userConsentId: {}, error: {}, details: {}]", userConsentId, error, details);
        userConsentRepository.findById(userConsentId)
            .ifPresent(userConsent -> {
                log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
                userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
                userConsent.setStatus(ConsentStatus.REFUSED);
                userConsent.setErrorCode(error);
                userConsent.setErrorDetail(details);
                userConsent = userConsentRepository.save(userConsent);

                // delete the requisition
                requisitionService.delete(UUID.fromString(userConsent.getRequisitionId()));

                // send consent denied event notification
                consentEventSender.sendConsentDenied(userConsent);
            });
    }
}
