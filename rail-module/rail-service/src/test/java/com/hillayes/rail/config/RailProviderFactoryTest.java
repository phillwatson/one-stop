package com.hillayes.rail.config;

import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.errors.RailNotFoundException;
import com.hillayes.rail.errors.RailsErrorCodes;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class RailProviderFactoryTest {
    @Inject
    @Any
    Instance<RailProviderApi> railProviderApis;

    @Inject
    RailProviderFactory railProviderFactory;

    @Test
    public void testGet() {
        railProviderApis.stream().forEach( railProviderApi ->
            assertEquals(railProviderApi, railProviderFactory.get(railProviderApi.getProviderId()))
        );
    }

    @Test
    public void testGetImplementation() {
        railProviderApis.stream()
            .forEach( railProviderApi ->
                assertEquals(railProviderApi, railProviderFactory.getImplementation(railProviderApi.getProviderId().name()))
        );
    }

    @Test
    public void testGetImplementation_NotFound() {
        RailNotFoundException exception = assertThrows(RailNotFoundException.class, () ->
            railProviderFactory.getImplementation("NOT_FOUND")
        );

        assertEquals(RailsErrorCodes.RAIL_NOT_FOUND, exception.getErrorCode());
        assertEquals("NOT_FOUND", exception.getParameter("rail-provider"));

    }

    @Test
    public void testGetAll() {
        assertEquals(railProviderApis.stream().count(), railProviderFactory.getAll().count());
    }
}
