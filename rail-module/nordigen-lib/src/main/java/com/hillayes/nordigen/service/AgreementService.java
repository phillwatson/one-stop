package com.hillayes.nordigen.service;

import com.hillayes.nordigen.api.AgreementApi;
import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.EndUserAgreementAccepted;
import com.hillayes.nordigen.model.EndUserAgreementRequest;
import com.hillayes.nordigen.model.PaginatedList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class AgreementService extends AbstractRailService {
    @Inject
    @RestClient
    AgreementApi agreementApi;

    public PaginatedList<EndUserAgreement> list(int limit,
                                                int offset) {
        log.debug("Listing agreements [limit: {}, offset: {}]", limit, offset);
        return agreementApi.list(limit, offset);
    }

    public EndUserAgreement create(EndUserAgreementRequest agreement) {
        log.debug("Creating agreement [institutionId: {}]", agreement.getInstitutionId());
        EndUserAgreement result = agreementApi.create(agreement);
        log.debug("Created agreement [institutionId: {}, id: {}]", agreement.getInstitutionId(), result.id);
        return result;
    }

    public Optional<EndUserAgreement> accept(String id,
                                             EndUserAgreementAccepted acceptance) {
        log.debug("Accepting agreement [id: {}]", id);
        try {
            return Optional.ofNullable(agreementApi.accept(id, acceptance));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<EndUserAgreement> get(String id) {
        log.debug("Retrieving agreement [id: {}]", id);
        try {
            return Optional.ofNullable(agreementApi.get(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Map<String, Object> delete(String id) {
        log.debug("Deleting agreement [id: {}]", id);
        try {
            return agreementApi.delete(id);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Map.of();
            }
            throw e;
        }
    }
}
