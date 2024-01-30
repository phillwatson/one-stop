package com.hillayes.rail.service;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.Institution;
import com.hillayes.rail.config.ServiceConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InstitutionService {
    @Inject
    RailProviderApi railProviderApi;

    @Inject
    ServiceConfiguration config;

    private Cache<CacheKey, List<Institution>> cacheByCountry;
    private Cache<String, Optional<Institution>> cacheById;

    @PostConstruct
    public void init() {
        cacheByCountry = new Cache<>(config.caches().institutions());
        cacheById = new Cache<>(config.caches().institutions());
    }

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
        CacheKey key = new CacheKey(countryCode, paymentsEnabled);
        return cacheByCountry.getValueOrCall(key, () ->
            railProviderApi.listInstitutions(countryCode, paymentsEnabled));
    }

    public Optional<Institution> get(String id) {
        return cacheById.getValueOrCall(id, () -> railProviderApi.getInstitution(id));
    }

    private record CacheKey(String countryCode, Boolean paymentsEnabled) {
    }
}
