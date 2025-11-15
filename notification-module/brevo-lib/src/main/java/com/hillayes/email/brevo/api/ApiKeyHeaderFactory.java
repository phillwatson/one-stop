package com.hillayes.email.brevo.api;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Supplies the API-Key auth header values for the Brevo email API.
 */
@Singleton
@Slf4j
public class ApiKeyHeaderFactory implements ClientHeadersFactory {
    // settings default so that this lib can be imported into other libs without this setting
    @ConfigProperty(name = "one-stop.email.brevo.api-key", defaultValue = "not-set")
    String API_KEY;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("api-key", API_KEY);
        result.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return result;
    }
}
