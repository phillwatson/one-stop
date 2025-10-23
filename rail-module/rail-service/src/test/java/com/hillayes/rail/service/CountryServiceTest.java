package com.hillayes.rail.service;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Country;
import com.hillayes.rail.repository.CountryRepository;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CountryServiceTest {
    private final CountryRepository countryRepository = mock();

    private final CountryService fixture = new CountryService(
        countryRepository
    );

    @Test
    public void testList() {
        // given: a collection of country configurations
        Collection<Country> countries = List.of(
            Country.builder().id("1").name("Country 1").flagUri("flag1.png").build(),
            Country.builder().id("2").name("Country 2").flagUri("flag2.png").build(),
            Country.builder().id("3").name("Country 3").flagUri("flag3.png").build()
        );
        when(countryRepository.getConfig()).thenReturn(countries);

        // when: the list of countries is requested
        Collection<Country> result = fixture.list();

        // then: the countries are returned
        assertNotNull(result);
        assertEquals(countries.size(), result.size());
    }

    @Test
    public void testGetCountry() {
        // given: a country exists
        Country country = Country.builder().id("1").name("Country 1").flagUri("flag1.png").build();
        when(countryRepository.getCountry(country.getId()))
            .thenReturn(Optional.of(country));

        // when: the country is requested
        Optional<Country> result = fixture.get(country.getId());

        // then: the country is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(country, result.get());
    }

    @Test
    public void testGetCountry_NotFound() {
        // given: the ID for an unknown country
        String countryId = UUID.randomUUID().toString();
        when(countryRepository.getCountry(countryId))
            .thenReturn(Optional.empty());

        // when: the country is requested
        Optional<Country> result = fixture.get(countryId);

        // then: NO country is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetLogo() {
        // given: a country exists
        Country country = Country.builder().id("1").name("Country 1").flagUri("/country-logos/it.png").build();
        when(countryRepository.getCountry(country.getId()))
            .thenReturn(Optional.of(country));

        // when: the country logo is requested
        Optional<InputStream> result = fixture.getLogo(country.getId());

        // then: the logo stream is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testGetLogo_NotFound() {
        // given: the ID for an unknown country
        String countryId = UUID.randomUUID().toString();
        when(countryRepository.getCountry(countryId))
            .thenReturn(Optional.empty());

        // when: the country logo is requested
        // then: NotFoundException
        assertThrows(NotFoundException.class, () -> fixture.getLogo(countryId));
    }

    @Test
    public void testGetLogo_NoLogoProvided() {
        // given: a country exists - with no logo
        Country country = Country.builder().id("1").name("Country 1").flagUri(null).build();
        when(countryRepository.getCountry(country.getId()))
            .thenReturn(Optional.of(country));

        // when: the country logo is requested
        Optional<InputStream> result = fixture.getLogo(country.getId());

        // then: NO logo stream is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
