package com.hillayes.rail.service;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InstitutionService {
    @Inject
    RailProviderFactory railProviderFactory;

    @Inject
    ServiceConfiguration config;

    private Cache<CacheKey, List<RailInstitution>> cacheByCountry;
    private Cache<String, Optional<RailInstitution>> cacheById;

    @PostConstruct
    public void init() {
        cacheByCountry = new Cache<>(config.caches().institutions());
        cacheById = new Cache<>(config.caches().institutions());
    }

    public List<RailInstitution> list(RailProvider railProvider,
                                      String countryCode,
                                      Boolean paymentsEnabled) {
        CacheKey key = new CacheKey(countryCode, paymentsEnabled);
        if (railProvider == null) {
            // return all institutions from all providers
            return cacheByCountry.getValueOrCall(key, () ->
                railProviderFactory.getAll()
                    .flatMap(api -> api.listInstitutions(countryCode, paymentsEnabled).stream())
                    .toList());
        }

        // return institutions from the specified provider
        RailProviderApi railProviderApi = railProviderFactory.get(railProvider);
        return cacheByCountry.getValueOrCall(key, () ->
            railProviderApi.listInstitutions(countryCode, paymentsEnabled));
    }

    public Optional<RailInstitution> get(RailProvider railProvider, String id) {
        return cacheById.getValueOrCall(id, () ->
            railProviderFactory.get(railProvider).getInstitution(id));
    }

    public Optional<RailInstitution> get(String id) {
        return cacheById.getValueOrCall(id, () -> railProviderFactory.getAll()
            .map(api -> api.getInstitution(id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
        );
    }

    private record CacheKey(String countryCode, Boolean paymentsEnabled) {
    }
}
