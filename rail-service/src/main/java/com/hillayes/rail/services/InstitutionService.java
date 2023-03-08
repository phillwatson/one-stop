package com.hillayes.rail.services;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.repository.InstitutionRepository;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class InstitutionService {
    @Inject
    @RestClient
    InstitutionRepository institutionRepository;

    private final Cache<CacheKey,List<Institution>> cache =
        new Cache<>(Duration.ofHours(5).toMillis());

    public List<Institution> list(String countryCode,
                                  Boolean paymentsEnabled) {
        CacheKey key = new CacheKey(countryCode, paymentsEnabled);
        return cache.getValueOrCall(key, () -> {
            List<Institution> list = institutionRepository.list(countryCode, paymentsEnabled);
            list.add(get("SANDBOXFINANCE_SFIN0000"));
            return list;
        });
    }

    public Institution get(String id) {
        return institutionRepository.get(id);
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class CacheKey {
        private final String countryCode;
        private final Boolean paymentsEnabled;
    }
}
