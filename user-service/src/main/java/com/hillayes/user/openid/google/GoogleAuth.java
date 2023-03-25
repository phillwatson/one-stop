package com.hillayes.user.openid.google;

import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.AuthProvider;
import com.hillayes.user.openid.AuthProviderNamed;
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

@ApplicationScoped
@Slf4j
public class GoogleAuth extends OpenIdAuth {
    @Inject
    @AuthProviderNamed(AuthProvider.GOOGLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @AuthProviderNamed(AuthProvider.GOOGLE)
    JwtValidator jwtValidator;

    @Inject
    @RestClient
    GoogleIdRestApi googleIdRestApi;

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
