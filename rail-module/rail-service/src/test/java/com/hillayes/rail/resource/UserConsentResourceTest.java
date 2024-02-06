package com.hillayes.rail.resource;

import com.hillayes.rail.api.domain.RailProvider;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserConsentResourceTest {
    @Test
    public void testResponseUri() {
        URI uri = UriBuilder
            .fromResource(UserConsentResource.class)
            .scheme("http")
            .host("192.2.2.2")
            .port(5555)
            .path(UserConsentResource.class, "consentResponse")
            .buildFromMap(Map.of("railProvider", RailProvider.NORDIGEN));

        assertEquals("http://192.2.2.2:5555/api/v1/rails/consents/response/NORDIGEN", uri.toString());
    }
}
