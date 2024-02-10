package com.hillayes.yapily.service;

import com.hillayes.yapily.api.ConsentsApi;
import com.hillayes.yapily.model.ApiResponseOfConsent;
import com.hillayes.yapily.model.Consent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class ConsentsService extends AbstractRailService {
    @Inject
    @RestClient
    ConsentsApi consentsApi;

    public Optional<Consent> getConsent(String consentId) {
        log.debug("Retrieving consent [id: {}]", consentId);
        try {
            ApiResponseOfConsent response = consentsApi.getConsent(consentId);
            return Optional.ofNullable(response.getData());
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public List<Consent> listConsents(UUID userId) {
        log.debug("Retrieving consents for user [id: {}]", userId);
        try {
            List<Consent> data = consentsApi.getConsents(null, List.of(userId), null,
                null, null, null, 0, 1000).getData();
            return data == null ? List.of() : data;
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return List.of();
            }
            throw e;
        }
    }

    public boolean deleteConsent(String consentId) {
        log.debug("Deleting consent [id: {}]", consentId);
        try {
            consentsApi.deleteConsent(consentId, true);
            return true;
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw e;
        }
    }
}
