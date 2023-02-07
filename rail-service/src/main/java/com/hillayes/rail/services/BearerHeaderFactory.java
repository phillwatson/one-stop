package com.hillayes.rail.services;

import com.hillayes.rail.model.ObtainJwtResponse;
import com.hillayes.rail.model.RefreshJwtResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

@Slf4j
public class BearerHeaderFactory implements ClientHeadersFactory {
    @ConfigProperty(name = "rails.secret.id")
    String SECRET_ID;
    @ConfigProperty(name = "rails.secret.key")
    String SECRET_KEY;

    @Inject
    @RestClient
    AuthService authService;

    private Token accessToken;
    private Token refreshToken;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("Authorization", "Bearer " + getAccessToken());
        return result;
    }


    private String getAccessToken() {
        if (accessToken == null) {
            log.info("Get new access and refresh token [secretId: {}]", SECRET_ID);
            ObtainJwtResponse response = authService.newToken(SECRET_ID, SECRET_KEY);
            accessToken = new Token(response.access, response.accessExpires);
            refreshToken = new Token(response.refresh, response.refreshExpires);
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
