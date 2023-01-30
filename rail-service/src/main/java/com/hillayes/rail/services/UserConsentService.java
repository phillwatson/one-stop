package com.hillayes.rail.services;

import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.model.*;
import com.hillayes.rail.repository.UserConsentRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class UserConsentService {
    private static final String REDIRECT_URL = "/api/v1/consents/accepted";
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

    public URI register(UUID userId, String institutionId) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);
        if (userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId).size() > 0) {
            throw new BadRequestException("Bank already registered");
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
        userConsent = userConsentRepository.saveAndFlush(userConsent);

        try {
            // create requisition
            log.debug("Requesting requisition [userId: {}, institutionId: {}: ref: {}]", userId, institutionId, userConsent.getId());
            Requisition requisition = requisitionService.create(RequisitionRequest.builder()
                    .institutionId(agreement.institutionId)
                    .agreement(agreement.id)
                    .userLanguage("EN")
                    .reference(userConsent.getId().toString())
                    .redirect(REDIRECT_URL)
                    .redirectImmediate(Boolean.FALSE)
                    .build());

            // record requisition
            userConsent.setRequisitionId(requisition.id.toString());
            userConsent.setStatus(ConsentStatus.WAITING);

            // return link for user consent
            log.debug("Returning consent link [userId: {}, institutionId: {}, link: {}]", userId, institutionId, requisition.link);
            return URI.create(requisition.link);
        } catch (Exception e) {
            log.error("Failed to register bank [userId: {}, institutionId: {}]", userId, institutionId, e);

            // delete the registration record
            userConsentRepository.delete(userConsent);
            throw new InternalServerErrorException("Failed to register bank " + institutionId);
        }
    }

    public void consentAccepted(UUID userBankId) {
        log.info("User's consent received [userBankId: {}]", userBankId);
        UserConsent userConsent = userConsentRepository.findById(userBankId)
                .orElseThrow(NotFoundException::new);

        log.debug("Recording consent [userId: {}, institutionId: {}, expires: {}]",
                userConsent.getUserId(), userConsent.getInstitutionId(), userConsent.getAgreementExpires());
        userConsent.setStatus(ConsentStatus.GIVEN);
    }
}
