package com.hillayes.yapily.service;

import com.hillayes.commons.Strings;
import com.hillayes.yapily.api.InstitutionsApi;
import com.hillayes.yapily.model.FeatureEnum;
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
    private static final Collection<FeatureEnum> PAYMENT_FEATURES = Set.of(
        FeatureEnum.INITIATE_DOMESTIC_PERIODIC_PAYMENT,
        FeatureEnum.INITIATE_DOMESTIC_SCHEDULED_PAYMENT,
        FeatureEnum.INITIATE_DOMESTIC_SINGLE_INSTANT_PAYMENT,
        FeatureEnum.INITIATE_DOMESTIC_SINGLE_PAYMENT,
        FeatureEnum.INITIATE_INTERNATIONAL_PERIODIC_PAYMENT,
        FeatureEnum.INITIATE_INTERNATIONAL_SCHEDULED_PAYMENT,
        FeatureEnum.INITIATE_INTERNATIONAL_SINGLE_PAYMENT
    );

    @Inject
    @RestClient
    InstitutionsApi institutionsApi;

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
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
            .filter(institution -> arePaymentsEnabled(institution) == paymentsEnabled)
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

    public boolean arePaymentsEnabled(Institution institution) {
        if (institution.getFeatures() == null) {
            return false;
        }

        return (institution.getFeatures().stream()
            .anyMatch(PAYMENT_FEATURES::contains));
    }
}
