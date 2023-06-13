package com.hillayes.rail.repository;

import com.hillayes.rail.model.ObtainJwtResponse;
import com.hillayes.rail.model.RefreshJwtResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

@ApplicationScoped
@Slf4j
public class BearerHeaderFactory implements ClientHeadersFactory {
    @ConfigProperty(name = "rails.secret.id")
    String SECRET_ID;
    @ConfigProperty(name = "rails.secret.key")
    String SECRET_KEY;

    @Inject
    @RestClient
    AuthRepository authService;

    private Token accessToken;
    private Token refreshToken;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
        result.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return result;
    }


    private String getAccessToken() {
        if (accessToken == null) {
            log.info("Get new access and refresh token [secretId: {}]", SECRET_ID);
            ObtainJwtResponse response = authService.newToken(SECRET_ID, SECRET_KEY);
            accessToken = new Token(response.getAccess(), response.getAccessExpires());
            refreshToken = new Token(response.getRefresh(), response.getRefreshExpires());
        }

        else if (accessToken.hasExpired()) {
            if (refreshToken.hasExpired()) {
                log.info("Refresh token expired [secretId: {}]", SECRET_ID);
                // obtain new token pair
                accessToken = null;
                refreshToken = null;
                return getAccessToken();
            }

            log.info("Refresh access token [secretId: {}]", SECRET_ID);
            RefreshJwtResponse response = authService.refreshToken(refreshToken.token);
            accessToken = new Token(response.access, response.accessExpires);
        }

        return accessToken.token;
    }

    private static class Token {
        private static final long EXPIRY_TOLERANCE = 1000;

        String token;
        long expires;

        Token(String token, Integer expiresInSecs) {
            this.token = token;
            this.expires = System.currentTimeMillis() + (expiresInSecs * 1000);
        }

        public boolean hasExpired() {
            return expires <= System.currentTimeMillis() - EXPIRY_TOLERANCE;
        }
    }
}
