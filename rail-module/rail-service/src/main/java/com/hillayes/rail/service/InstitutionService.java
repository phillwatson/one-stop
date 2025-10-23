package com.hillayes.rail.service;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
public class InstitutionService {
    private final RailProviderFactory railProviderFactory;
    private final ServiceConfiguration config;

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
        if (railProvider == null) {
            // return all institutions from all providers
            return railProviderFactory.getAll()
                .flatMap(api -> {
                    CacheKey key = new CacheKey(api.getProviderId(), countryCode, paymentsEnabled);
                    return cacheByCountry.getValueOrCall(key, k ->
                        api.listInstitutions(k.countryCode(), k.paymentsEnabled())
                    ).stream();
                }).toList();
        }

        // return institutions from the specified provider
        CacheKey key = new CacheKey(railProvider, countryCode, paymentsEnabled);
        return cacheByCountry.getValueOrCall(key, k ->
            railProviderFactory.get(k.railProvider()).listInstitutions(k.countryCode(), k.paymentsEnabled()));
    }

    public Optional<RailInstitution> get(RailProvider railProvider, String id) {
        return cacheById.getValueOrCall(id, k ->
            railProviderFactory.get(railProvider).getInstitution(k));
    }

    public Optional<RailInstitution> get(String id) {
        return cacheById.getValueOrCall(id, k -> railProviderFactory.getAll()
            .map(api -> api.getInstitution(k))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
        );
    }

    private record CacheKey(RailProvider railProvider, String countryCode, Boolean paymentsEnabled) {
    }
}
