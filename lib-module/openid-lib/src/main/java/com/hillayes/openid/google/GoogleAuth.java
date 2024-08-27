package com.hillayes.openid.google;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import java.net.URI;

import static com.hillayes.openid.AuthProvider.GOOGLE;

/**
 * Provides the OpenIdAuth implementation for the Google auth-provider. The
 * instance will be initialised with the Open-ID configuration and IdTokenValidator
 * appropriate for Google.
 */
@ApplicationScoped
@NamedAuthProvider(GOOGLE)
@Slf4j
public class GoogleAuth implements OpenIdAuth {
    @Inject
    @NamedAuthProvider(GOOGLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(GOOGLE)
    OpenIdConfigResponse openIdConfig;

    @Inject
    @NamedAuthProvider(GOOGLE)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(GOOGLE)
    OpenIdTokenApi openIdTokenApi;

    @Override
    public boolean isFor(AuthProvider authProvider) {
        return authProvider == GOOGLE;
    }

    @Override
    public boolean isEnabled() {
        return config.clientId().isPresent() && config.clientSecret().isPresent();
    }

    public URI initiateLogin(String clientState) {
        return config.clientId()
            .map(clientId -> UriBuilder.fromUri(openIdConfig.authorizationEndpoint)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("scope", "openid profile email")
                .queryParam("redirect_uri", config.redirectUri())
                .queryParam("state", clientState)
                .build())
            .orElse(null);
    }

    public JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException {
        log.debug("Exchanging auth code for tokens [authCode: {}]", authCode);
        if ((config.clientId().isEmpty()) || (config.clientSecret().isEmpty())) {
            return null;
        }

        TokenExchangeRequest request = TokenExchangeRequest.builder()
            .grantType("authorization_code")
            .redirectUri(config.redirectUri())
            .code(authCode)
            .clientId(config.clientId().get())
            .clientSecret(config.clientSecret().get())
            .build();

        TokenExchangeResponse response = openIdTokenApi.exchangeToken(request);
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        return idTokenValidator.verify(response.idToken);
    }

    public String toString() {
        return "OpenIdAuth["+ config.configUri() + "]";
    }
}
