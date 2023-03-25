package com.hillayes.user.openid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.rest.OpenIdConfigResponse;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URL;

/**
 * A collection of Producer/Factory methods to inject and configure the OpenID
 * auth provider implementations. Each configuration can be injected using the
 * Qualifier @AuthProviderNamed. For example; to inject a JwtValidator for
 * validating Google ID-Tokens:
 *
 * <pre>
 *   @Inject
 *   @AuthProviderNamed(AuthProvider.GOOGLE)
 *   JwtValidator jwtValidator;
 * </pre>
 */
@Slf4j
public class OpenIdProvider {
    @Inject
    ObjectMapper mapper;

    @Inject
    public OpenIdConfiguration openIdConfiguration;

    /**
     * Provides the open-id configuration for Google-ID.
     */
    @Produces
    @AuthProviderNamed(AuthProvider.GOOGLE)
    @Singleton
    public OpenIdConfiguration.AuthConfig googleConfig() {
        log.debug("Retrieving Google OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.GOOGLE);
    }

    @Produces
    @AuthProviderNamed(AuthProvider.APPLE)
    @Singleton
    public OpenIdConfiguration.AuthConfig appleConfig() {
        log.debug("Retrieving Apple OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.APPLE);
    }

    @Produces
    @AuthProviderNamed(AuthProvider.GOOGLE)
    @Singleton
    public JwtValidator googleValidator(@AuthProviderNamed(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig config) throws IOException {
        return jwtValidator(config);
    }

    @Produces
    @AuthProviderNamed(AuthProvider.APPLE)
    @Singleton
    public JwtValidator appleValidator(@AuthProviderNamed(AuthProvider.APPLE) OpenIdConfiguration.AuthConfig config) throws IOException {
        return jwtValidator(config);
    }

    private JwtValidator jwtValidator(OpenIdConfiguration.AuthConfig config) throws IOException {
        log.debug("Retrieving OpenId Config [url: {}]", config.configUri());
        URL url = new URL(config.configUri());
        OpenIdConfigResponse openIdConfig = mapper.readValue(url, OpenIdConfigResponse.class);

        log.debug("Using key-set [url: {}]", openIdConfig.jwksUri);
        return new JwtValidator(openIdConfig.jwksUri, openIdConfig.issuer, config.clientId());
    }
}
