package com.hillayes.user.openid.google;

import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.AuthProvider;
import com.hillayes.user.openid.NamedAuthProvider;
import com.hillayes.user.openid.OpenIdAuth;
import com.hillayes.user.openid.OpenIdConfiguration;
import com.hillayes.user.openid.rest.TokenExchangeRequest;
import com.hillayes.user.openid.rest.TokenExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Provides the OpenIdAuth implementation for the Google auth-provider. The
 * instance will be initialised with the Open-ID configuration and JwtValidator
 * appropriate for Google.
 */
@ApplicationScoped
@NamedAuthProvider(AuthProvider.GOOGLE)
@Slf4j
public class GoogleAuth extends OpenIdAuth {
    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    JwtValidator jwtValidator;

    @Inject
    @RestClient
    GoogleIdRestApi googleIdRestApi;

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

        TokenExchangeResponse response = googleIdRestApi.exchangeToken(request);
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        return jwtValidator.verify(response.idToken);
    }

    public String toString() {
        return "OpenIdAuth["+ config.configUri() + "]";
    }
}
