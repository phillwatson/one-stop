package com.hillayes.rail.services;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementAccepted;
import com.hillayes.rail.model.EndUserAgreementRequest;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.repository.AgreementRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AgreementService {
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

    public EndUserAgreement accept(UUID id,
                                   EndUserAgreementAccepted acceptance) {
        return agreementRepository.accept(id, acceptance);
    }

    public EndUserAgreement get(UUID id) {
        return agreementRepository.get(id);
    }

    public Map<String,Object> delete(UUID id) {
        return agreementRepository.delete(id);
    }
}
