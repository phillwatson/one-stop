package com.hillayes.rail.service;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.repository.InstitutionRepository;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InstitutionService extends AbstractRailService {
    @Inject
    @RestClient
    InstitutionRepository institutionRepository;

    @Inject
    ServiceConfiguration config;

    private Cache<CacheKey,List<Institution>> cache;

    @PostConstruct
    public void init() {
        cache = new Cache<>(config.caches().institutions());
    }

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
        CacheKey key = new CacheKey(countryCode, paymentsEnabled);
        return cache.getValueOrCall(key, () -> {
            List<Institution> list = institutionRepository.list(countryCode, paymentsEnabled);
            get("SANDBOXFINANCE_SFIN0000")
                .ifPresent(list::add);

            list.forEach(entry -> entry.paymentsEnabled = paymentsEnabled);
            return list;
        });
    }

    public Optional<Institution> get(String id) {
        try {
            return Optional.ofNullable(institutionRepository.get(id));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class CacheKey {
        private final String countryCode;
        private final Boolean paymentsEnabled;
    }
}
