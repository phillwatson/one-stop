package com.hillayes.openid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * A collection of Producer/Factory methods to inject and configure the OpenID
 * auth provider implementations. Each configuration can be injected using the
 * Qualifier @NamedAuthProvider. For example; to inject a IdTokenValidator for
 * validating Google ID-Tokens:
 *
 * <pre>
 *   \@Inject
 *   \@NamedAuthProvider(AuthProvider.GOOGLE)
 *   IdTokenValidator idTokenValidator;
 * </pre>
 */
@Slf4j
public class OpenIdFactory {
    /**
     * An ObjectMapper instance. Is configured to ignore unknown properties (by disabling
     * the DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES). Also configured to format
     * dates and time in ISO-8601 (by disabling the SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).
     */
    @Inject
    ObjectMapper mapper;

    /**
     * The LOCAL configuration (i.e. from application.yaml) containing configuration properties
     * for all OpenId providers supported by the application.
     * <p>
     * From this the auth provider's own configuration can be retrieved.
     * @see #openApiConfig(OpenIdConfiguration.AuthConfig)
     */
    @Inject
    public OpenIdConfiguration openIdConfiguration;

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
     * Returns the open-id configuration from Google's "well-known" configuration URL,
     * taken from the given local configuration.
     * <p>
     * See https://accounts.google.com/.well-known/openid-configuration
     *
     * @param config the local configuration for Google auth-provider.
     * @return Google's own configuration properties.
     * @throws IOException if the auth-provider's configuration cannot be read.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    @Singleton
    public OpenIdConfigResponse googleOpenApiConfig(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig config) throws IOException {
        return openApiConfig(config);
    }

    /**
     * Returns a REST client for the Google's auth-token exchange API. The URI of
     * this API endpoint is taken from the given open-id configuration, read from
     * Google's own "well-known" configuration.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    @Singleton
    public OpenIdTokenApi googleRestApi(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the Google
     * auth-provider. The validator will use the Json Web Key-Set referenced by the
     * URI found in Google's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    @Singleton
    public IdTokenValidator googleValidator(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig config,
                                            @NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(config, openIdConfig);
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
     * Returns the open-id configuration from Apple's "well-known" configuration URL,
     * taken from the given local configuration.
     * <p>
     * See https://appleid.apple.com/.well-known/openid-configuration
     *
     * @param config the local configuration for Apple auth-provider.
     * @return Apple's own configuration properties.
     * @throws IOException if the auth-provider's configuration cannot be read.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public OpenIdConfigResponse appleOpenApiConfig(@NamedAuthProvider(AuthProvider.APPLE) OpenIdConfiguration.AuthConfig config) throws IOException {
        return openApiConfig(config);
    }

    /**
     * Returns a REST client for the Apple's auth-token exchange API. The URI of
     * this API endpoint is taken from the given open-id configuration, read from
     * Apple's own "well-known" configuration.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public OpenIdTokenApi appleRestApi(@NamedAuthProvider(AuthProvider.APPLE) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the Apple
     * auth-provider. The validator will use the Json Web Key-Set referenced by the
     * URI found in Apple's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public IdTokenValidator appleValidator(@NamedAuthProvider(AuthProvider.APPLE) OpenIdConfiguration.AuthConfig config,
                                           @NamedAuthProvider(AuthProvider.APPLE) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(config, openIdConfig);
    }

    private IdTokenValidator idTokenValidator(OpenIdConfiguration.AuthConfig config,
                                            OpenIdConfigResponse openIdConfig) {
        log.debug("Using key-set [url: {}]", openIdConfig.jwksUri);
        return new IdTokenValidator(openIdConfig.jwksUri, openIdConfig.issuer, config.clientId());
    }

    private OpenIdConfigResponse openApiConfig(OpenIdConfiguration.AuthConfig config) throws IOException {
        log.debug("Retrieving OpenId Config [url: {}]", config.configUri());

        URL url = new URL(config.configUri());
        return mapper.readValue(url, OpenIdConfigResponse.class);
    }

    private OpenIdTokenApi openIdRestApi(OpenIdConfigResponse openIdConfig) {
        log.debug("Creating OpenId REST API [url: {}]", openIdConfig.tokenEndpoint);

        URI baseUri = URI.create(openIdConfig.tokenEndpoint);
        return RestClientBuilder.newBuilder()
            .baseUri(baseUri)
            .build(OpenIdTokenApi.class);
    }
}
