package com.hillayes.rail.services;

import com.hillayes.rail.model.Institution;
import com.hillayes.rail.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class InstitutionService {
    @Inject
    @RestClient
    InstitutionRepository institutionRepository;

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
        List<Institution> list = institutionRepository.list(countryCode, paymentsEnabled);
        list.add(get("SANDBOXFINANCE_SFIN0000"));
        return list;
    }

    public Institution get(String id) {
        return institutionRepository.get(id);
    }
}
