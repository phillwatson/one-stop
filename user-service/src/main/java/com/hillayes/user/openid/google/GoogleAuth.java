package com.hillayes.user.openid.google;

import com.hillayes.auth.jwt.JwtValidator;
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
import javax.inject.Named;

@ApplicationScoped
@Slf4j
public class GoogleAuth extends OpenIdAuth {
    @Inject
    @Named("googleConfig")
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @Named("googleValidator")
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
            .clientSecret(config.clientSecret())
            .build();

//        TokenExchangeResponse response = googleIdRestApi.exchangeToken(request);
//        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);
//
//        return jwtValidator.verify(response.idToken);
        return null;
    }
}
