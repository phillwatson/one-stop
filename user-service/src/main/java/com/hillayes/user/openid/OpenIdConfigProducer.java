package com.hillayes.user.openid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.rest.OpenIdConfigResponse;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URL;

@Slf4j
public class OpenIdConfigProducer {
    @Inject
    ObjectMapper mapper;

    @Inject
    public OpenIdConfiguration openIdConfiguration;

    @Produces
    @Named("googleConfig")
    @Singleton
    public OpenIdConfiguration.AuthConfig googleConfig() {
        log.info("Retrieving Google OpenId Config");
        return openIdConfiguration.authConfigs().get("google");
    }

    @Produces
    @Named("appleConfig")
    @Singleton
    public OpenIdConfiguration.AuthConfig appleConfig() {
        log.info("Retrieving Apple OpenId Config");
        return openIdConfiguration.authConfigs().get("apple");
    }

    @Produces
    @Named("googleValidator")
    @Singleton
    public JwtValidator googleValidator(@Named("googleConfig") OpenIdConfiguration.AuthConfig config) throws IOException {
        return jwtValidator(config);
    }

    @Produces
    @Named("appleValidator")
    @Singleton
    public JwtValidator appleValidator(@Named("appleConfig") OpenIdConfiguration.AuthConfig config) throws IOException {
        return jwtValidator(config);
    }

    private JwtValidator jwtValidator(OpenIdConfiguration.AuthConfig config) throws IOException {
        log.info("Retrieving OpenId Config [url: {}]", config.configUri());
        URL url = new URL(config.configUri());
        OpenIdConfigResponse openIdConfig = mapper.readValue(url, OpenIdConfigResponse.class);

        log.info("Using key-set [url: {}]", openIdConfig.jwksUri);
        return new JwtValidator(openIdConfig.jwksUri, openIdConfig.issuer, config.clientId());
    }
}
