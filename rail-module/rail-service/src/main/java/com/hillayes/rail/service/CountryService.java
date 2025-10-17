package com.hillayes.rail.service;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Country;
import com.hillayes.rail.repository.CountryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CountryService {
    private final CountryRepository countryRepository;

    public Collection<Country> list() {
        log.info("List countries");
        Collection<Country> result = countryRepository.getConfig();
        log.info("List countries [size: {}]", result.size());
        return result;
    }

    public Optional<Country> get(String id) {
        log.info("Get country [id: {}]", id);

        Optional<Country> result = countryRepository.getCountry(id);
        log.info("Get country [id: {}, found: {}]", id, result.isPresent());
        return result;
    }

    public Optional<InputStream> getLogo(String id) {
        log.info("Get country logo [id: {}]", id);
        Country country = get(id).orElseThrow(() -> new NotFoundException("Country", id));
        if (country.getFlagUri() == null) {
            return Optional.empty();
        }

        InputStream resource = getClass().getResourceAsStream(country.getFlagUri());
        return Optional.ofNullable(resource);
    }
}
