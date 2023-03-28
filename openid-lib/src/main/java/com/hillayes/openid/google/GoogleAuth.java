package com.hillayes.openid.google;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Provides the OpenIdAuth implementation for the Google auth-provider. The
 * instance will be initialised with the Open-ID configuration and IdTokenValidator
 * appropriate for Google.
 */
@ApplicationScoped
@NamedAuthProvider(AuthProvider.GOOGLE)
@Slf4j
public class GoogleAuth implements OpenIdAuth {
    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdTokenApi openIdTokenApi;

    @Override
    public boolean isFor(AuthProvider authProvider) {
        return authProvider == AuthProvider.GOOGLE;
    }

    public JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException {
        TokenExchangeRequest request = TokenExchangeRequest.builder()
            .grantType("authorization_code")
            .redirectUri(config.redirectUri())
            .code(authCode)
            .clientId(config.clientId())
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
