package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.net.Gateway;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.Agreement;
import com.hillayes.rail.api.domain.Institution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.errors.BankRegistrationException;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.resource.UserConsentResource;
import com.hillayes.rail.scheduled.PollConsentJobbingTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class UserConsentService {
    // The number of days for which account access will be agreed
    private final static int ACCESS_VALID_FOR_DAYS = 90;

    /**
     * The account scopes for which all consents are granted access.
     */
    private final static List<String> CONSENT_SCOPES = List.of("balances", "details", "transactions");

    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    RailProviderApi railProviderApi;

    @Inject
    PollConsentJobbingTask pollConsentJobbingTask;

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
        return userConsentRepository.lock(consentId);
    }

    public URI register(UUID userId, RailProvider railProvider, String institutionId, URI callbackUri) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);

        Institution institution = railProviderApi.getInstitution(institutionId)
            .orElseThrow(() -> new NotFoundException("Institution", institutionId));

        // read any existing consent
        UserConsent userConsent = getUserConsent(userId, institutionId).orElse(null);

        // if the existing consent has not expired
        if ((userConsent != null) && (userConsent.getStatus() != ConsentStatus.EXPIRED)) {
            throw new BankAlreadyRegisteredException(userId, institutionId);
        }

        try {
            // construct URI from the consent resource callback method
            URI registrationCallbackUrl = UriBuilder
                .fromResource(UserConsentResource.class)
                .scheme(gateway.getScheme())
                .host(gateway.getHost())
                .port(gateway.getPort())
                .path(UserConsentResource.class, "consentResponse")
                .build();

            // generate a random reference and register the agreement with the rail
            String reference = UUID.randomUUID().toString();
            Agreement agreement = railProviderApi.register(reference, institution, registrationCallbackUrl);

            // record agreement in a consent record - with the reference
            log.debug("Recording agreement [userId: {}, institutionId: {}]", userId, institutionId);
            if (userConsent == null) {
                userConsent = UserConsent.builder()
                    .dateCreated(Instant.now())
                    .userId(userId)
                    .provider(railProvider)
                    .institutionId(institution.getId())
                    .build();
            }
            userConsent.setAgreementId(agreement.getId());
            userConsent.setReference(reference);
            userConsent.setMaxHistory(agreement.getMaxHistory());
            userConsent.setAgreementExpires(agreement.getDateExpires());
            userConsent.setCallbackUri(callbackUri.toString());
            userConsent.setStatus(ConsentStatus.WAITING);
            userConsent = userConsentRepository.saveAndFlush(userConsent);

            // send consent initiated event notification
            consentEventSender.sendConsentInitiated(userConsent);

            // return link for user consent
            log.debug("Returning consent link [userId: {}, institutionId: {}, link: {}]",
                userId, institutionId, agreement.getAgreementLink());
            return agreement.getAgreementLink();
        } catch (Exception e) {
            throw new BankRegistrationException(userId, institutionId, e);
        }
    }

    public URI consentGiven(String consentReference) {
        log.info("User's consent received [userConsentId: {}]", consentReference);
        UserConsent userConsent = userConsentRepository.findByReference(consentReference)
            .orElseThrow(() -> new NotFoundException("UserConsent.reference", consentReference));

        log.debug("Recording consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
            userConsent.getUserId(), consentReference, userConsent.getInstitutionId(), userConsent.getAgreementExpires());

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

    public URI consentDenied(String consentReference, String error, String details) {
        log.info("User's consent denied [userConsentId: {}, error: {}, details: {}]", consentReference, error, details);
        UserConsent userConsent = userConsentRepository.findByReference(consentReference)
            .orElseThrow(() -> new NotFoundException("UserConsent.reference", consentReference));

        log.debug("Updating consent [userId: {}, userConsentId: {}, institutionId: {}, expires: {}]",
        userConsent.getUserId(), consentReference, userConsent.getInstitutionId(), userConsent.getAgreementExpires());

        deleteAgreement(userConsent);

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
                (userConsent.getStatus() != ConsentStatus.DENIED)) {
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
            railProviderApi.deleteAgreement(userConsent.getAgreementId());
        } catch (Exception e) {
            // log and continue
            log.info("Error whilst deleting user's agreement [userId: {}, agreementId: {}]",
                userConsent.getUserId(), userConsent.getAgreementId(), e);
        }
    }
}
