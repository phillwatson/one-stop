package com.hillayes.rail.service;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementAccepted;
import com.hillayes.rail.model.EndUserAgreementRequest;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.repository.AgreementRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

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

    public EndUserAgreement accept(String id,
                                   EndUserAgreementAccepted acceptance) {
        return agreementRepository.accept(id, acceptance);
    }

    public EndUserAgreement get(String id) {
        return agreementRepository.get(id);
    }

    public Map<String,Object> delete(String id) {
        return agreementRepository.delete(id);
    }
}
