package com.hillayes.rail.repository;

import com.hillayes.rail.config.SupportedCountries;
import com.hillayes.rail.domain.Country;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class CountryRepository {
    @Inject
    SupportedCountries countries;

    public Collection<Country> getCountries() {
        log.info("Get all countries");
        Collection<Country> result = countries.countries()
                .stream()
                .map(entry -> Country.builder()
                        .id(entry.id())
                        .name(entry.name())
                        .build())
                .collect(Collectors.toSet());

        log.info("Get all countries [size: {}]", result.size());
        return result;
    }

    public Optional<Country> getCountry(String id) {
        log.info("Get country [id: {}]", id);

        Optional<Country> result = countries.countries()
                .stream()
                .filter(c -> c.id().equalsIgnoreCase(id))
                .findAny()
                .map(c -> Country.builder()
                                .id(c.id())
                                .name(c.name())
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
