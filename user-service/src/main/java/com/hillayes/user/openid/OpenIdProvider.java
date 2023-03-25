package com.hillayes.user.openid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.google.GoogleAuth;
import com.hillayes.user.openid.google.GoogleIdRestApi;
import com.hillayes.user.openid.rest.OpenIdConfigResponse;
import com.hillayes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URL;

/**
 * A collection of Producer/Factory methods to inject and configure the OpenID
 * auth provider implementations. Each configuration can be injected using the
 * Qualifier @NamedAuthProvider. For example; to inject a JwtValidator for
 * validating Google ID-Tokens:
 *
 * <pre>
 *   &amp;Inject
 *   &amp;NamedAuthProvider(AuthProvider.GOOGLE)
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
     * Provides the OpenIdAuth implementation for the Google auth-provider. The
     * returned instance will be initialised with the Open-ID configuration and
     * JwtValidator appropriate for Google.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    @Singleton
    public OpenIdAuth googleAuth(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig config,
                                 @NamedAuthProvider(AuthProvider.GOOGLE) JwtValidator jwtValidator,
                                 @RestClient GoogleIdRestApi googleIdRestApi,
                                 UserRepository userRepository,
                                 PasswordCrypto passwordCrypto) {
        log.debug("Retrieving Google OpenId Authenticator");
        return new GoogleAuth(config, jwtValidator, googleIdRestApi, userRepository, passwordCrypto);
    }

    /**
     * Provides the open-id configuration for the Google auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    @Singleton
    public OpenIdConfiguration.AuthConfig googleConfig() {
        log.debug("Retrieving Google OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.GOOGLE);
    }

    /**
     * Provides the open-id configuration for the Apple auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public OpenIdConfiguration.AuthConfig appleConfig() {
        log.debug("Retrieving Apple OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.APPLE);
    }

    /**
     * Provides the JwtValidator instance for validating ID-Tokens from the Google
     * auth-provider. The validator will use the Json Web Key-Set referenced by the
     * URI found in Google's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    @Singleton
    public JwtValidator googleValidator(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig config) throws IOException {
        return jwtValidator(config);
    }

    @Produces
    /**
     * Provides the JwtValidator instance for validating ID-Tokens from the Apple
     * auth-provider. The validator will use the Json Web Key-Set referenced by the
     * URI found in Apple's "well-known" config resource.
     */
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public JwtValidator appleValidator(@NamedAuthProvider(AuthProvider.APPLE) OpenIdConfiguration.AuthConfig config) throws IOException {
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
