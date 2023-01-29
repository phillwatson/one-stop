package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Country;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;

@QuarkusTest
public class CountryRepositoryTest {
    @Inject
    CountryRepository countryRepository;

    @Test
    public void testGetAll() {
        Collection<Country> countries = countryRepository.getCountries();
        assertNotNull(countries);
        assertEquals(3, countries.size());
    }

    @Test
    public void testGet_Found() {
        Optional<Country> gb = countryRepository.getCountry("GB");
        assertTrue(gb.isPresent());
        assertEquals("Great Britain", gb.get().getName());
    }

    @Test
    public void testGet_NotFound() {
        Optional<Country> result = countryRepository.getCountry("XX");
        assertTrue(result.isEmpty());
    }
}
