package com.hillayes.yapily.api;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Supplies the Authorization header values for the Yapily rail API.
 * It uses the registered secret ID and key for the Auth header of
 * ongoing request.
 */
public class BasicHeaderFactory implements ClientHeadersFactory {
    @ConfigProperty(name = "one-stop.yapily.secret.id", defaultValue = "not-set")
    String SECRET_ID;
    @ConfigProperty(name = "one-stop.yapily.secret.key", defaultValue = "not-set")
    String SECRET_KEY;

    private String CREDENTIALS = null;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedToken());
        result.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return result;
    }

    private String encodedToken() {
        if (CREDENTIALS == null) {
            String token = SECRET_ID + ":" + SECRET_KEY;
            byte[] encodedBytes = Base64.getEncoder().encode(token.getBytes(StandardCharsets.ISO_8859_1));
            CREDENTIALS = new String(encodedBytes, StandardCharsets.ISO_8859_1);
        }
        return CREDENTIALS;
    }
}
