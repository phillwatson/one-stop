package com.hillayes.openid.github;

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

import java.net.URI;
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
 * IMPORTANT:
 * For authentication to work we need access to the user's email address registered
 * with the open-id provider. For GitHub that requires explicit action on the user's
 * part to allow access to their email address.
 * In their GitHub email settings (https://github.com/settings/emails), uncheck the
 * "Keep my email address private".
 * Then, in their GitHub profile (https://github.com/settings/profile), select the
 * email address in the "Public email" drop-down.
 * Having authenticated once, they can make their email address private again.
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
    OpenIdConfigResponse openIdConfig;

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

    public URI initiateLogin(String clientState) {
        return UriBuilder.fromUri(openIdConfig.authorizationEndpoint)
            .queryParam("response_type", "code")
            .queryParam("client_id", config.clientId())
            .queryParam("scope", "openid profile email user:email")
            .queryParam("redirect_uri", config.redirectUri())
            .queryParam("state", clientState)
            .build();
    }

    public JwtClaims exchangeAuthToken(String authCode) {
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
