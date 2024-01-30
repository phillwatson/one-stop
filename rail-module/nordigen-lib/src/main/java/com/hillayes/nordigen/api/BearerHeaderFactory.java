package com.hillayes.nordigen.api;

import com.hillayes.nordigen.model.ObtainJwtResponse;
import com.hillayes.nordigen.model.RefreshJwtResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Supplies the Authorization header values for the Nordigen rail API.
 * It uses the secret ID and key registered with the rail in order to obtain
 * access and refresh tokens. The access token will be added the Auth
 * bearer header of ongoing request.
 *
 * If the access token has expired, the refresh token will be used to obtain
 * a new one.
 */
@Slf4j
public class BearerHeaderFactory implements ClientHeadersFactory {
    @ConfigProperty(name = "one-stop.nordigen.secret.id")
    String SECRET_ID;
    @ConfigProperty(name = "one-stop.nordigen.secret.key")
    String SECRET_KEY;

    @Inject
    @RestClient
    AuthApi authApi;

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
            ObtainJwtResponse response = authApi.newToken(SECRET_ID, SECRET_KEY);
            accessToken = new Token(response.getAccess(), response.getAccessExpires());
            refreshToken = new Token(response.getRefresh(), response.getRefreshExpires());
            log.debug("Access and refresh token retrieved [secretId: {}]", SECRET_ID);
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
            RefreshJwtResponse response = authApi.refreshToken(refreshToken.token);
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