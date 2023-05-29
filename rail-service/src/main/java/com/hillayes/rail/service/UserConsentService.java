package com.hillayes.rail.service;

import com.hillayes.commons.net.Network;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.errors.BankRegistrationException;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.model.*;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.UserConsentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
    AccountRepository accountRepository;

    @Inject
    InstitutionService institutionService;

    @Inject
    AgreementService agreementService;

    @Inject
    RequisitionService requisitionService;

    @Inject
    ConsentEventSender consentEventSender;

    public Page<UserConsent> listConsents(UUID userId,
                                          int page,
                                          int pageSize) {
        log.info("Listing user's banks [userId: {}]", userId);
        Page<UserConsent> result = userConsentRepository.findByUserId(userId, PageRequest.of(page, pageSize));
        log.debug("Listing user's banks [userId: {}, size: {}]", userId, result.getNumberOfElements());
        return result;
    }

    public Optional<UserConsent> getUserConsent(UUID userId, String institutionId) {
        log.info("Looking for user's consent record [userId: {}, institutionId: {}]", userId, institutionId);
        return userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId).stream()
            .filter(consent -> consent.getStatus() != ConsentStatus.CANCELLED)
            .filter(consent -> consent.getStatus() != ConsentStatus.DENIED)
            .findFirst();
    }

    public URI register(UUID userId, String institutionId, URI callbackUri) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);
        if (getUserConsent(userId, institutionId).isPresent()) {
            throw new BankAlreadyRegisteredException(userId, institutionId);
        }

        Institution institution = institutionService.get(institutionId)
            .orElseThrow(() -> new NotFoundException("Institution", institutionId));

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
            .dateCreated(Instant.now())
            .userId(userId)
            .institutionId(agreement.institutionId)
            .agreementId(agreement.id)
            .maxHistory(agreement.maxHistoricalDays)
            .agreementExpires(expires)
            .callbackUri(callbackUri.toString())
            .status(ConsentStatus.INITIATED)
            .build();

        // save to generate ID
        userConsent = userConsentRepository.saveAndFlush(userConsent);

        try {
            // create requisition
            log.debug("Requesting requisition [userId: {}, institutionId: {}: ref: {}]", userId, institutionId, userConsent.getId());

            String registrationCallbackUrl = config.callbackUrl().replace("{{host-ip}}", Network.getMyIpAddress());
            log.debug("Registration callback URL: {}", registrationCallbackUrl);

            Requisition requisition = requisitionService.create(RequisitionRequest.builder()
                .institutionId(agreement.institutionId)
                .agreement(agreement.id)
                .userLanguage("EN")
                .reference(userConsent.getId().toString())
                .redirect(registrationCallbackUrl)
                .redirectImmediate(Boolean.FALSE)
                .build());

            // record requisition
            userConsent.setRequisitionId(requisition.id);
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

    public URI consentGiven(UUID userConsentId) {
        log.info("User's consent received [userConsentId: {}]", userConsentId);
        UserConsent userConsent = userConsentRepository.findById(userConsentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", userConsentId));

        log.debug("Recording consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
            userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        URI redirectUrl = URI.create(userConsent.getCallbackUri());
        userConsent.setStatus(ConsentStatus.GIVEN);
        userConsent.setDateGiven(Instant.now());
        userConsent.setCallbackUri(null);
        userConsent = userConsentRepository.save(userConsent);

        // send consent-given event notification
        consentEventSender.sendConsentGiven(userConsent);

        return redirectUrl;
    }

    public URI consentDenied(UUID userConsentId, String error, String details) {
        log.info("User's consent denied [userConsentId: {}, error: {}, details: {}]", userConsentId, error, details);
        UserConsent userConsent = userConsentRepository.findById(userConsentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", userConsentId));

        log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
        userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        // delete the requisition - and the associated agreement
        requisitionService.delete(userConsent.getRequisitionId());

        URI redirectUrl = URI.create(userConsent.getCallbackUri());
        userConsent.setStatus(ConsentStatus.DENIED);
        userConsent.setDateDenied(Instant.now());
        userConsent.setCallbackUri(null);
        userConsent.setErrorCode(error);
        userConsent.setErrorDetail(details);
        userConsent = userConsentRepository.save(userConsent);

        // send consent-denied event notification
        consentEventSender.sendConsentDenied(userConsent);

        return redirectUrl;
    }

    public void consentCancelled(UUID userConsentId) {
        log.info("User's consent cancelled [userConsentId: {}]", userConsentId);
        userConsentRepository.findById(userConsentId)
            .ifPresent(userConsent -> {
                log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
                    userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
                userConsent.setStatus(ConsentStatus.CANCELLED);
                userConsent.setDateCancelled(Instant.now());
                userConsent = userConsentRepository.save(userConsent);

                // delete the requisition - and the associated agreement
                requisitionService.delete(userConsent.getRequisitionId());

                // delete the account records
                accountRepository.findByUserConsentId(userConsent.getId())
                    .forEach(accountRepository::delete);

                // send consent denied event notification
                consentEventSender.sendConsentCancelled(userConsent);
            });
    }
}
