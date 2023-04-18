package com.hillayes.rail.service;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementAccepted;
import com.hillayes.rail.model.EndUserAgreementRequest;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.repository.AgreementRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AgreementService extends AbstractRailService {
    @Inject
    @RestClient
    AgreementRepository agreementRepository;

    public PaginatedList<EndUserAgreement> list(int limit,
                                                int offset) {
        return agreementRepository.list(limit, offset);
    }

    public EndUserAgreement create(EndUserAgreementRequest agreement) {
        return agreementRepository.create(agreement);
    }

    public Optional<EndUserAgreement> accept(String id,
                                             EndUserAgreementAccepted acceptance) {
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