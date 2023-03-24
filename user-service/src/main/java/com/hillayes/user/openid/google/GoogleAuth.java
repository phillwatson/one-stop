package com.hillayes.user.openid.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.OpenIdAuth;
import com.hillayes.user.openid.rest.OpenIdConfigResponse;
import com.hillayes.user.openid.rest.TokenExchangeRequest;
import com.hillayes.user.openid.rest.TokenExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;

@ApplicationScoped
@Slf4j
public class GoogleAuth extends OpenIdAuth {
    @Inject
    GoogleIdConfig config;

    @Inject
    @RestClient
    GoogleIdRestApi googleIdRestApi;

    @Inject
    ObjectMapper mapper;

    private JwtValidator jwtValidator;

    @PostConstruct
    public void init() throws IOException {
        log.info("Retrieving Google OpenId Config [url: {}]", config.configUri());
        URL url = new URL(config.configUri());
        OpenIdConfigResponse openIdConfig = mapper.readValue(url, OpenIdConfigResponse.class);

        log.info("Using Google ID key-set [url: {}]", openIdConfig.jwksUri);
        jwtValidator = new JwtValidator(openIdConfig.jwksUri, openIdConfig.issuer, config.clientId());
    }

    public JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException {
        TokenExchangeRequest request = TokenExchangeRequest.builder()
            .grantType("authorization_code")
            .redirectUri(config.redirectUri())
            .code(authCode)
            .clientId(config.clientId())
            .clientSecret(config.clientSecret())
            .build();

        TokenExchangeResponse response = googleIdRestApi.exchangeToken(request);
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        return jwtValidator.verify(response.idToken);
    }
}
