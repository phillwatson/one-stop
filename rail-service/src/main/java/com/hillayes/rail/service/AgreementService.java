package com.hillayes.rail.service;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementAccepted;
import com.hillayes.rail.model.EndUserAgreementRequest;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.repository.AgreementRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class AgreementService extends AbstractRailService {
    @Inject
    @RestClient
    AgreementRepository agreementRepository;

    public PaginatedList<EndUserAgreement> list(int limit,
                                                int offset) {
        log.debug("Listing agreements [limit: {}, offset: {}]", limit, offset);
        return agreementRepository.list(limit, offset);
    }

    public EndUserAgreement create(EndUserAgreementRequest agreement) {
        log.debug("Creating agreement [institutionId: {}]", agreement.getInstitutionId());
        EndUserAgreement result = agreementRepository.create(agreement);
        log.debug("Created agreement [institutionId: {}, id: {}]", agreement.getInstitutionId(), result.id);
        return result;
    }

    public Optional<EndUserAgreement> accept(String id,
                                             EndUserAgreementAccepted acceptance) {
        log.debug("Accepting agreement [id: {}]", id);
        try {
            return Optional.ofNullable(agreementRepository.accept(id, acceptance));
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
            return Optional.ofNullable(agreementRepository.get(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Map<String, Object> delete(String id) {
        log.debug("Deleting requisition [id: {}]", id);
        try {
            return agreementRepository.delete(id);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Map.of();
            }
            throw e;
        }
    }
}
