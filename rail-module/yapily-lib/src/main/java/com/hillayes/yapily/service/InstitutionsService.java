package com.hillayes.yapily.service;

import com.hillayes.commons.Strings;
import com.hillayes.yapily.api.InstitutionsApi;
import com.hillayes.yapily.model.Institution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;

@ApplicationScoped
@Slf4j
public class InstitutionsService extends AbstractRailService {
    @Inject
    @RestClient
    InstitutionsApi institutionsApi;

    public List<Institution> list(String countryCode) {
        List<Institution> list = institutionsApi.getInstitutions().getData();
        if (list == null) {
            return List.of();
        }

        return list.stream()
            .filter(institution -> Strings.isNotBlank(institution.getName()))
            .filter(institution -> institution.getCountries() != null)
            .filter(institution -> institution.getCountries().stream()
                .anyMatch(country -> countryCode.equalsIgnoreCase(country.getCountryCode2()))
            )
            .sorted(Comparator.comparing(Institution::getName))
            .toList();
    }

    public Optional<Institution> get(String id) {
        try {
            return Optional.ofNullable(institutionsApi.getInstitution(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
