package com.hillayes.rail.services;

import com.hillayes.rail.repository.CountryRepository;
import com.hillayes.rail.repository.domain.Country;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Optional;

@Singleton
@Slf4j
public class CountryService {
    @Inject
    CountryRepository countryRepository;

    public Collection<Country> list() {
        log.info("List countries");
        Collection<Country> result = countryRepository.getCountries();
        log.info("List countries [size: {}]", result.size());
        return result;
    }

    public Optional<Country> get(String id) {
        log.info("Get country [id: {}]", id);

        Optional<Country> result = countryRepository.getCountry(id);
        log.info("Get country [id: {}, found: {}]", id, result.isPresent());
        return result;
    }
}
