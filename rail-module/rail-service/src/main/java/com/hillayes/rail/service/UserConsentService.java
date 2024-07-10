package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.net.Gateway;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.ConsentResponse;
import com.hillayes.rail.api.domain.RailAgreement;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.errors.BankRegistrationException;
import com.hillayes.rail.errors.DeleteRailConsentException;
import com.hillayes.rail.errors.RegistrationNotFoundException;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.resource.UserConsentResource;
import com.hillayes.rail.scheduled.ConsentTimeoutJobbingTask;
import com.hillayes.rail.scheduled.PollConsentJobbingTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
@Transactional
@Slf4j
public class UserConsentService {
    // those statuses in which an existing user-consent can be renewed/registered
    private static final Set<ConsentStatus> RENEWABLE_STATUSES = Set.of(
        ConsentStatus.EXPIRED, ConsentStatus.SUSPENDED, ConsentStatus.DENIED,
        ConsentStatus.TIMEOUT, ConsentStatus.CANCELLED
    );

    private static final Set<ConsentStatus> ACTIVE_STATUSES = Set.of(
        ConsentStatus.INITIATED, ConsentStatus.GIVEN
    );

    @Inject
    ServiceConfiguration configuration;

    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    InstitutionService institutionService;

    @Inject
    RailProviderFactory railProviderFactory;

    @Inject
    PollConsentJobbingTask pollConsentJobbingTask;

    @Inject
    ConsentTimeoutJobbingTask consentTimeoutJobbingTask;

    @Inject
    ConsentEventSender consentEventSender;

    @Inject
    Gateway gateway;

    public Page<UserConsent> listConsents(UUID userId, int page, int pageSize) {
        log.info("Listing user's banks [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        Page<UserConsent> result = userConsentRepository.findByUserId(userId, page, pageSize);
        log.debug("Listing user's banks [userId: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
            userId, page, pageSize, result.getContentSize(), result.getTotalCount());
        return result;
    }

    /**
     * Returns the consent record for the identified user and institution.
     * @param userId the user identifier.
     * @param institutionId the institution identifier.
     * @return the user-consent record, or an empty result.
     */
    public Optional<UserConsent> getUserConsent(UUID userId, String institutionId) {
        log.info("Looking for user's consent record [userId: {}, institutionId: {}]", userId, institutionId);
        return userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId);
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
        return userConsentRepository.lock(consentId);
    }

    public URI register(UUID userId, String institutionId, URI callbackUri) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);

        RailInstitution institution = institutionService.get(institutionId)
            .orElseThrow(() -> new NotFoundException("Institution", institutionId));

        // read any existing consent
        UserConsent userConsent = getUserConsent(userId, institutionId).orElse(null);

        // if the existing consent is not of a renewable status
        if ((userConsent != null) &&
            (! RENEWABLE_STATUSES.contains(userConsent.getStatus()))) {
            throw new BankAlreadyRegisteredException(userId, institutionId);
        }

        RailProviderApi railProviderApi = railProviderFactory.get(institution.getProvider());
        try {
            // construct URI from the consent resource callback method
            URI registrationCallbackUrl = UriBuilder
                .fromResource(UserConsentResource.class)
                .scheme(gateway.getScheme())
                .host(gateway.getHost())
                .port(gateway.getPort())
                .path(UserConsentResource.class, "consentResponse")
                .buildFromMap(Map.of("railProvider", institution.getProvider()));

            // generate a random reference
            String reference = UUID.randomUUID().toString();

            // register the agreement with the rail
            RailAgreement agreement = railProviderApi.register(userId, institution, registrationCallbackUrl, reference);
            try {
                // record agreement in a consent record - with the reference
                log.debug("Recording agreement [userId: {}, institutionId: {}, reference: {}]", userId, institutionId, reference);
                if (userConsent == null) {
                    userConsent = UserConsent.builder()
                        .dateCreated(Instant.now())
                        .userId(userId)
                        .provider(institution.getProvider())
                        .institutionId(institution.getId())
                        .build();
                }
                userConsent.setAgreementId(agreement.getId());
                userConsent.setReference(reference);
                userConsent.setMaxHistory(agreement.getMaxHistory());
                userConsent.setAgreementExpires(agreement.getDateExpires());
                userConsent.setDateGiven(null);
                userConsent.setDateDenied(null);
                userConsent.setCallbackUri(callbackUri.toString());
                userConsent.setStatus(ConsentStatus.INITIATED);
                userConsent = userConsentRepository.saveAndFlush(userConsent);

                // send consent initiated event notification
                consentEventSender.sendConsentInitiated(userConsent);

                // queue job to check for consent timeout
                consentTimeoutJobbingTask.queueJob(userConsent, configuration.consentTimeout());

                // return link for user consent
                log.debug("Returning consent link [userId: {}, institutionId: {}, link: {}]",
                    userId, institutionId, agreement.getAgreementLink());
                return agreement.getAgreementLink();
            } catch (Exception e) {
                try {
                    railProviderApi.deleteAgreement(agreement.getId());
                } catch (Exception ignore) { }
                throw e;
            }
        } catch (Exception e) {
            throw new BankRegistrationException(userId, institutionId, e);
        }
    }

    /**
     * Called after the configured consent-timeout period to check if the consent
     * registration is still pending and, if so, mark it as timed out. It will also
     * delete the rail agreement, and issue a consent-timed-out event.
     *
     * @param consentId the identifier of the consent to be checked.
     * @return true if the consent was found and marked as timed out.
     */
    public boolean registrationTimeout(UUID consentId) {
        log.info("Checking consent timeout [consentId: {}]", consentId);
        UserConsent userConsent = userConsentRepository.findByIdOptional(consentId).orElse(null);

        // if consent not found - or is no longer in INITIATED state
        if ((userConsent == null) || (userConsent.getStatus() != ConsentStatus.INITIATED)) {
            return false;
        }

        log.info("Consent has timed-out [consentId: {}]", consentId);
        try {
            deleteAgreement(userConsent);
        } catch (DeleteRailConsentException ignore) {}

        userConsent.setStatus(ConsentStatus.TIMEOUT);
        userConsent.setCallbackUri(null);
        userConsent = userConsentRepository.save(userConsent);

        // send consent-timed-out event notification
        consentEventSender.sendConsentTimedOut(userConsent);
        return true;
    }

    public URI consentGiven(RailProviderApi railProvider, ConsentResponse consentResponse) {
        log.info("User's consent received [railProvider: {}, reference: {}]",
            railProvider.getProviderId(), consentResponse.getConsentReference());

        UserConsent userConsent = userConsentRepository.findByReference(consentResponse.getConsentReference())
            .orElseThrow(() -> new RegistrationNotFoundException(railProvider.getProviderId(), consentResponse));

        log.debug("Recording consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
            userConsent.getUserId(), userConsent.getId(), userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        RailAgreement agreement = railProvider
            .getAgreement(userConsent.getAgreementId())
            .orElse(null);

        URI redirectUrl = URI.create(userConsent.getCallbackUri());
        userConsent.setStatus(ConsentStatus.GIVEN);
        userConsent.setAgreementExpires(agreement == null ? null : agreement.getDateExpires());
        userConsent.setDateGiven(Instant.now());
        userConsent.setCallbackUri(null);
        userConsent = userConsentRepository.save(userConsent);

        // queue a job to verify the consent and poll account for data
        pollConsentJobbingTask.queueJob(userConsent.getId());

        // send consent-given event notification
        consentEventSender.sendConsentGiven(userConsent);

        return redirectUrl;
    }

    public URI consentDenied(RailProviderApi railProvider, ConsentResponse consentResponse) {
        log.info("User's consent denied [response: {}]", consentResponse);

        UserConsent userConsent = userConsentRepository.findByReference(consentResponse.getConsentReference())
            .orElseThrow(() -> new RegistrationNotFoundException(railProvider.getProviderId(), consentResponse));

        log.debug("Updating consent [userId: {}, consentId: {}, institutionId: {}, expires: {}]",
        userConsent.getUserId(), userConsent.getId(), userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        deleteAgreement(userConsent);

        URI redirectUri = UriBuilder
            .fromPath(userConsent.getCallbackUri())
            .queryParam("error", consentResponse.getErrorCode())
            .queryParam("details", consentResponse.getErrorDescription())
            .build();

        userConsent.setStatus(ConsentStatus.DENIED);
        userConsent.setDateDenied(Instant.now());
        userConsent.setCallbackUri(null);
        userConsent.setErrorCode(consentResponse.getErrorCode());
        userConsent.setErrorDetail(consentResponse.getErrorDescription());
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

                deleteAgreement(userConsent);

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

                deleteAgreement(userConsent);

                // send consent expired event notification
                consentEventSender.sendConsentExpired(userConsent);
            });
    }

    /**
     * Marks the consent record as cancelled but does not delete it, or the
     * account records associated with it.
     * @param userConsentId the identifier of the consent to be cancelled.
     * @param purge if true, the associated accounts and transactions will be deleted.
     */
    public void consentCancelled(UUID userConsentId, boolean purge) {
        log.info("User's consent cancelled [userConsentId: {}, purge: {}]", userConsentId, purge);
        userConsentRepository.findByIdOptional(userConsentId)
            .filter(userConsent -> userConsent.getStatus() != ConsentStatus.CANCELLED)
            .ifPresent(userConsent -> {
                log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
                    userConsent.getUserId(), userConsentId, userConsent.getInstitutionId(), userConsent.getAgreementExpires());
                userConsent.setStatus(ConsentStatus.CANCELLED);
                userConsent.setDateCancelled(Instant.now());

                if (purge) {
                    // will cascade delete accounts, balances, transactions and category selectors
                    userConsentRepository.delete(userConsent);
                } else {
                    userConsent = userConsentRepository.save(userConsent);
                }

                deleteAgreement(userConsent);

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
                (userConsent.getStatus() != ConsentStatus.DENIED) &&
                (userConsent.getStatus() != ConsentStatus.TIMEOUT)) {
                deleteAgreement(userConsent);

                // send consent cancelled event notification
                consentEventSender.sendConsentCancelled(userConsent);
            }

            // will cascade delete accounts, balances and transactions
            userConsentRepository.delete(userConsent);
        });
    }

    private void deleteAgreement(UserConsent userConsent) {
        try {
            // delete the rail's record of the agreement
            RailProviderApi railProviderApi = railProviderFactory.get(userConsent.getProvider());
            railProviderApi.deleteAgreement(userConsent.getAgreementId());
        } catch (Exception e) {
            // log and continue
            log.info("Error whilst deleting user's agreement [userId: {}, agreementId: {}]",
                userConsent.getUserId(), userConsent.getAgreementId(), e);
            throw new DeleteRailConsentException(userConsent, e);
        }
    }
}
