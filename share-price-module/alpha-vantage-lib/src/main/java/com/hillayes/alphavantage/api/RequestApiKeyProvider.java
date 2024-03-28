package com.hillayes.alphavantage.api;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;

public class RequestApiKeyProvider implements ClientRequestFilter {
    @ConfigProperty(name = "one-stop.alpha-vantage.secret.key", defaultValue = "not-set")
    String API_KEY;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.setUri(UriBuilder
            .fromUri(requestContext.getUri())
            .queryParam("apikey", API_KEY)
            .build());
    }
}
