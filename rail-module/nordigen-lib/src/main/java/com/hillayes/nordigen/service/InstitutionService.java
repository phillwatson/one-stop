package com.hillayes.nordigen.service;

import com.hillayes.nordigen.api.InstitutionApi;
import com.hillayes.nordigen.model.Institution;
import com.hillayes.nordigen.model.InstitutionDetail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InstitutionService extends AbstractRailService {
    @Inject
    @RestClient
    InstitutionApi institutionApi;

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
        List<Institution> list = institutionApi.list(countryCode);

        // add the sandbox institution to all results
        get("SANDBOXFINANCE_SFIN0000")
            .map(detail -> {
                Institution institution = new Institution();
                institution.id = detail.id;
                institution.name = detail.name;
                institution.bic = detail.bic;
                institution.logo = detail.logo;
                institution.countries = detail.countries;
                institution.transactionTotalDays = detail.transactionTotalDays;
                return institution;
            })
            .ifPresent(list::add);

        list.sort(null);
        return list;
    }

    public Optional<InstitutionDetail> get(String id) {
        try {
            return Optional.ofNullable(institutionApi.get(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
