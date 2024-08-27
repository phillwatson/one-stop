package com.hillayes.openid.linkedin;

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

import static com.hillayes.openid.AuthProvider.LINKEDIN;

/**
 * Provides the OpenIdAuth implementation for the LinkedIn auth-provider. The
 * instance will be initialised with the Open-ID configuration and IdTokenValidator
 * appropriate for LinkedIn.
 *
 * https://learn.microsoft.com/en-us/linkedin/consumer/integrations/self-serve/sign-in-with-linkedin-v2
 */
@ApplicationScoped
@NamedAuthProvider(LINKEDIN)
@Slf4j
public class LinkedInAuth implements OpenIdAuth {
    @Inject
    @NamedAuthProvider(LINKEDIN)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(LINKEDIN)
    OpenIdConfigResponse openIdConfig;

    @Inject
    @NamedAuthProvider(LINKEDIN)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(LINKEDIN)
    OpenIdTokenApi openIdTokenApi;

    @Override
    public boolean isFor(AuthProvider authProvider) {
        return authProvider == LINKEDIN;
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

        TokenExchangeResponse response = openIdTokenApi.exchangeToken("authorization_code",
            config.clientId().get(), config.clientSecret().get(), authCode, config.redirectUri());
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        return idTokenValidator.verify(response.idToken);
    }

    public String toString() {
        return "OpenIdAuth["+ config.configUri() + "]";
    }
}
