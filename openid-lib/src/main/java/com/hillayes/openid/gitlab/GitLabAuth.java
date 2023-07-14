package com.hillayes.openid.gitlab;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

/**
 * Provides the OpenIdAuth implementation for the GitLab auth-provider. The
 * instance will be initialised with the Open-ID configuration and IdTokenValidator
 * appropriate for GitLab.
 *
 * https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#web-application-flow
 */
@ApplicationScoped
@NamedAuthProvider(AuthProvider.GITLAB)
@Slf4j
public class GitLabAuth implements OpenIdAuth {
    @Inject
    @NamedAuthProvider(AuthProvider.GITLAB)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(AuthProvider.GITLAB)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(AuthProvider.GITLAB)
    OpenIdTokenApi openIdTokenApi;

    @Override
    public boolean isFor(AuthProvider authProvider) {
        return authProvider == AuthProvider.GITLAB;
    }

    public JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException {
        log.debug("Exchanging auth code for tokens [authCode: {}]", authCode);
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
