package com.hillayes.openid.github;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import java.util.Map;

import static com.hillayes.openid.AuthProvider.GITHUB;

/**
 * Provides the OpenIdAuth implementation for the GitHub auth-provider. The
 * instance will be initialised with the Open-ID configuration and IdTokenValidator
 * appropriate for GitHub.
 *
 * The GitHub API does not fully support the Open-ID Connect API in that, it does
 * not return an ID-Token with user profile data. Instead, it returns just an
 * access-token. With that we can call the GitHub REST API to retrieve the user's
 * profile data.
 *
 * https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#web-application-flow
 */
@ApplicationScoped
@NamedAuthProvider(GITHUB)
@Slf4j
public class GitHubAuth implements OpenIdAuth {
    @Inject
    @NamedAuthProvider(GITHUB)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(GITHUB)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(GITHUB)
    OpenIdTokenApi openIdTokenApi;

    @Inject
    private GitHubApiClient gitHubApiClient;

    @Override
    public boolean isFor(AuthProvider authProvider) {
        return authProvider == GITHUB;
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

        // token exchange will only retrieve an access-token
        TokenExchangeResponse response = openIdTokenApi.exchangeToken(request);
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        // use the access-token to retrieve the user profile data
        Map<String,Object> userProfile = gitHubApiClient.getUserProfile("Bearer " + response.accessToken);

        // use the user's node-id (or id) as their subject claim
        Object subject = userProfile.get("node_id");
        if (subject == null) {
            subject = userProfile.get("id");
        }

        // map the claims to a JwtClaims for a return value
        JwtClaims result = new JwtClaims();
        result.setSubject(subject.toString());
        result.setIssuer("https://github.com");
        userProfile.forEach(result::setClaim); // overwrite previous issuer and subject if present in map

        return result;
    }

    public String toString() {
        return "OpenIdAuth["+ config.configUri() + "]";
    }
}
