package com.hillayes.rail.service.yapily;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.service.AbstractRailService;
import com.hillayes.yapily.api.InstitutionsApi;
import com.hillayes.yapily.model.ApiListResponseOfInstitution;
import com.hillayes.yapily.model.Country;
import com.hillayes.yapily.model.FeatureEnum;
import com.hillayes.yapily.model.Institution;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class InstitutionService extends AbstractRailService {
    @Inject
    @RestClient
    InstitutionsApi institutionsApi;

    @Inject
    ServiceConfiguration config;

    private Cache<CacheKey, List<Institution>> cacheByCountry;
    private Cache<String, Institution> cacheById;

    @PostConstruct
    public void init() {
        cacheByCountry = new Cache<>(config.caches().institutions());
        cacheById = new Cache<>(config.caches().institutions());
    }

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
        CacheKey key = new CacheKey(countryCode, paymentsEnabled);
        return cacheByCountry.getValueOrCall(key, () -> {
            ApiListResponseOfInstitution response = institutionsApi.getInstitutions();
            if ((response == null) || (response.getData() == null)) {
                return Collections.emptyList();
            }

            return response.getData().stream()
                .filter(institution -> {
                    Set<Country> countries = institution.getCountries();
                    return countries != null && countries.stream()
                        .anyMatch(c -> countryCode.equals(c.getCountryCode2()));
                })
                .filter(institution ->
                    (!paymentsEnabled) ||
                        ((institution.getFeatures() != null) &&
                         (institution.getFeatures().contains(FeatureEnum.CREATE_DOMESTIC_SINGLE_PAYMENT)))
                )
                .toList();
        });
    }

    public Optional<Institution> get(String id) {
        try {
            return Optional.ofNullable(cacheById.getValueOrCall(id, () -> institutionsApi.getInstitution(id)));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private record CacheKey(String countryCode, Boolean paymentsEnabled) {
    }
}
