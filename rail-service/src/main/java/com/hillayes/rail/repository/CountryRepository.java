package com.hillayes.rail.repository;

import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.Country;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class CountryRepository {
    private final ServiceConfiguration config;

    public Collection<Country> getConfig() {
        log.info("Get all countries");
        Collection<Country> result = config.countries()
            .stream()
            .map(entry -> Country.builder()
                .id(entry.id())
                .name(entry.name())
                .flagUri(entry.flagUri().orElse(null))
                .build())
            .collect(Collectors.toSet());

        log.info("Get all countries [size: {}]", result.size());
        return result;
    }

    public Optional<Country> getCountry(String id) {
        log.info("Get country [id: {}]", id);

        Optional<Country> result = config.countries()
            .stream()
            .filter(c -> c.id().equalsIgnoreCase(id))
            .findAny()
            .map(c -> Country.builder()
                .id(c.id())
                .name(c.name())
                .flagUri(c.flagUri().orElse(null))
                .build()
            );

        if (result.isEmpty()) {
            log.info("Get country - not found [id: {}]", id);
        } else {
            log.info("Get country [id: {}, name: {}]", id, result.get().getName());
        }

        return result;
    }
}
