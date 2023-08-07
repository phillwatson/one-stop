package com.hillayes.rail.service;

import com.hillayes.commons.net.Network;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.errors.BankRegistrationException;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.model.*;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.resource.UserConsentResource;
import com.hillayes.rail.scheduled.PollConsentJobbingTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

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
    // The number of days for which account access will be agreed
    private final static int ACCESS_VALID_FOR_DAYS = 2;

    /**
     * The account scopes for which all consents are granted access.
     */
    private final static List<String> CONSENT_SCOPES = List.of("balances", "details", "transactions");

    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    InstitutionService institutionService;

    @Inject
    AgreementService agreementService;

    @Inject
    RequisitionService requisitionService;

    @Inject
    PollConsentJobbingTask pollConsentJobbingTask;

    @Inject
    ConsentEventSender consentEventSender;

    public Page<UserConsent> listConsents(UUID userId, int page, int pageSize) {
        log.info("Listing user's banks [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        Page<UserConsent> result = userConsentRepository.findByUserId(userId, page, pageSize);
        log.debug("Listing user's banks [userId: {}, page: {}, pageSize: {}, size: {}]",
            userId, page, pageSize, result.getNumberOfElements());
        return result;
    }

    public Optional<UserConsent> getUserConsent(UUID userId, String institutionId) {
        log.info("Looking for user's consent record [userId: {}, institutionId: {}]", userId, institutionId);
        return userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId).stream()
            .filter(consent -> consent.getStatus() != ConsentStatus.CANCELLED)
            .filter(consent -> consent.getStatus() != ConsentStatus.DENIED)
            .findFirst();
    }

    public Optional<UserConsent> getUserConsent(UUID consentId) {
        log.info("Get user's consent record [consentId: {}]", consentId);
        return userConsentRepository.findByIdOptional(consentId);
    }

    /**
     * Obtain a pessimistic lock on the identified UserConsent.
     *
     * @param consentId the user-consent identifier.
     * @return the locked user-consent, or an empty result.
     */
    public Optional<UserConsent> lockUserConsent(UUID consentId) {
        log.info("Locking user's consent record [consentId: {}]", consentId);
        return userConsentRepository.findByIdOptional(consentId, LockModeType.PESSIMISTIC_WRITE);
    }

    public URI register(UUID userId, String institutionId, URI callbackUri) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);

        Institution institution = institutionService.get(institutionId)
            .orElseThrow(() -> new NotFoundException("Institution", institutionId));

        // read any existing consent
        UserConsent userConsent = getUserConsent(userId, institutionId).orElse(null);

        // if the existing consent has not expired
        if ((userConsent != null) && (userConsent.getStatus() != ConsentStatus.EXPIRED)) {
            throw new BankAlreadyRegisteredException(userId, institutionId);
        }

        // create agreement
        log.debug("Requesting agreement [userId: {}, institutionId: {}]", userId, institutionId);
        EndUserAgreement agreement = agreementService.create(EndUserAgreementRequest.builder()
            .institutionId(institutionId)
            .accessScope(CONSENT_SCOPES)
            .maxHistoricalDays(institution.transactionTotalDays)
            .accessValidForDays(ACCESS_VALID_FOR_DAYS)
            .build());

        // calculate expiry data
        Instant expires = Instant.now()
            .truncatedTo(ChronoUnit.DAYS)
            .plus(agreement.accessValidForDays, ChronoUnit.DAYS);

        // record agreement in a consent record
        log.debug("Recording agreement [userId: {}, institutionId: {}, expires: {}]",
            userId, institutionId, expires);
        if (userConsent == null) {
            userConsent = UserConsent.builder()
                .dateCreated(Instant.now())
                .userId(userId)
                .institutionId(agreement.institutionId)
                .build();
        }
        userConsent.setAgreementId(agreement.id);
        userConsent.setMaxHistory(agreement.maxHistoricalDays);
        userConsent.setAgreementExpires(expires);
        userConsent.setCallbackUri(callbackUri.toString());
        userConsent.setStatus(ConsentStatus.INITIATED);

        userConsent = userConsentRepository.saveAndFlush(userConsent);

        try {
            // create requisition
            log.debug("Requesting requisition [userId: {}, institutionId: {}: ref: {}]",
                userId, institutionId, userConsent.getId());

            URI registrationCallbackUrl = UriBuilder
                .fromResource(UserConsentResource.class)
                .path(UserConsentResource.class, "consentResponse")
                .scheme("http")
                .host(Network.getMyIpAddress())
                .port(9876)
                .build();

            log.debug("Registration callback URL: {}", registrationCallbackUrl);

            Requisition requisition = requisitionService.create(RequisitionRequest.builder()
                .institutionId(agreement.institutionId)
                .agreement(agreement.id)
                .accountSelection(Boolean.FALSE)
                .userLanguage("EN")
                .redirectImmediate(Boolean.FALSE)
                .redirect(registrationCallbackUrl.toString())
                // reference is returned in callback
                // - allows us to identify the consent record
                .reference(userConsent.getId().toString())
                .build());

            // record requisition in the consent record
            userConsent.setRequisitionId(requisition.id);
            userConsent.setStatus(ConsentStatus.WAITING);
            userConsent = userConsentRepository.save(userConsent);

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
        UserConsent userConsent = userConsentRepository.findByIdOptional(userConsentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", userConsentId));

        log.debug("Recording consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
            userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        URI redirectUrl = URI.create(userConsent.getCallbackUri());
        userConsent.setStatus(ConsentStatus.GIVEN);
        userConsent.setDateGiven(Instant.now());
        userConsent.setCallbackUri(null);
        userConsent = userConsentRepository.save(userConsent);

        // queue a job to verify the consent and poll account for data
        pollConsentJobbingTask.queueJob(userConsent.getId());

        // send consent-given event notification
        consentEventSender.sendConsentGiven(userConsent);

        return redirectUrl;
    }

    public URI consentDenied(UUID userConsentId, String error, String details) {
        log.info("User's consent denied [userConsentId: {}, error: {}, details: {}]", userConsentId, error, details);
        UserConsent userConsent = userConsentRepository.findByIdOptional(userConsentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", userConsentId));

        log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
        userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        deleteRequisition(userConsent);

        URI redirectUri = UriBuilder
            .fromPath(userConsent.getCallbackUri())
            .queryParam("error", error)
            .queryParam("details", details)
            .build();

        userConsent.setStatus(ConsentStatus.DENIED);
        userConsent.setDateDenied(Instant.now());
        userConsent.setCallbackUri(null);
        userConsent.setErrorCode(error);
        userConsent.setErrorDetail(details);
        userConsent = userConsentRepository.save(userConsent);

        // send consent-denied event notification
        consentEventSender.sendConsentDenied(userConsent);

        return redirectUri;
    }

    public void consentSuspended(UUID userConsentId) {
        log.info("User's consent suspended [userConsentId: {}]", userConsentId);
        userConsentRepository.findByIdOptional(userConsentId)
            .filter(userConsent -> userConsent.getStatus() != ConsentStatus.SUSPENDED)
            .ifPresent(userConsent -> {
                log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
                    userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
                userConsent.setStatus(ConsentStatus.SUSPENDED);
                userConsent = userConsentRepository.save(userConsent);

                deleteRequisition(userConsent);

                // send consent suspended event notification
                consentEventSender.sendConsentSuspended(userConsent);
            });
    }

    public void consentExpired(UUID userConsentId) {
        log.info("User's consent expired [userConsentId: {}]", userConsentId);
        userConsentRepository.findByIdOptional(userConsentId)
            .filter(userConsent -> userConsent.getStatus() != ConsentStatus.EXPIRED)
            .ifPresent(userConsent -> {
                log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
                    userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
                userConsent.setStatus(ConsentStatus.EXPIRED);
                userConsent = userConsentRepository.save(userConsent);

                deleteRequisition(userConsent);

                // send consent expired event notification
                consentEventSender.sendConsentExpired(userConsent);
            });
    }

    /**
     * Marks the consent record as cancelled but does not delete it, or the
     * account records associated with it.
     * @param userConsentId the identifier of the consent to be cancelled.
     */
    public void consentCancelled(UUID userConsentId) {
        log.info("User's consent cancelled [userConsentId: {}]", userConsentId);
        userConsentRepository.findByIdOptional(userConsentId)
            .filter(userConsent -> userConsent.getStatus() != ConsentStatus.CANCELLED)
            .ifPresent(userConsent -> {
                log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
                    userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
                userConsent.setStatus(ConsentStatus.CANCELLED);
                userConsent.setDateCancelled(Instant.now());
                userConsent = userConsentRepository.save(userConsent);

                deleteRequisition(userConsent);

                // send consent cancelled event notification
                consentEventSender.sendConsentCancelled(userConsent);
            });
    }

    /**
     * Deletes all user-consents for the identified user. It will cascade to the
     * requisitions, end-user agreement, accounts, balances and transactions records.
     * This is called when the user is deleted.
     *
     * @param userId the user whose consents are to be deleted.
     */
    public void deleteAllConsents(UUID userId) {
        log.info("Deleting all user's consent records [userId: {}]", userId);
        userConsentRepository.findByUserId(userId).forEach(userConsent -> {
            log.debug("Deleting user consent [id: {}, institutionId: {}]",
                userConsent.getId(), userConsent.getInstitutionId());

            if ((userConsent.getStatus() != ConsentStatus.CANCELLED) &&
                (userConsent.getStatus() != ConsentStatus.EXPIRED) &&
                (userConsent.getStatus() != ConsentStatus.SUSPENDED) &&
                (userConsent.getStatus() != ConsentStatus.DENIED)) {
                deleteRequisition(userConsent);

                // send consent cancelled event notification
                consentEventSender.sendConsentCancelled(userConsent);
            }

            // will cascade delete accounts, balances and transactions
            userConsentRepository.delete(userConsent);
        });
    }

    private void deleteRequisition(UserConsent userConsent) {
        try {
            // delete the requisition - and the associated agreement
            requisitionService.delete(userConsent.getRequisitionId());
        } catch (Exception e) {
            // log and continue
            log.info("Error whilst deleting user's requisition [userId: {}, requisitionId: {}]",
                userConsent.getUserId(), userConsent.getRequisitionId(), e);
        }

        try {
            // delete any remaining agreement record - just in case
            agreementService.delete(userConsent.getAgreementId());
        } catch (Exception e) {
            // log and continue
            log.info("Error whilst deleting user's agreement [userId: {}, agreementId: {}]",
                userConsent.getUserId(), userConsent.getAgreementId(), e);
        }
    }
}
