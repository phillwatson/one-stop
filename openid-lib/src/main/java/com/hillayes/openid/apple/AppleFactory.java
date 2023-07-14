package com.hillayes.openid.apple;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

import java.io.IOException;

/**
 * A collection of Producer/Factory methods to inject and configure the OpenID
 * auth provider implementations. Each configuration can be injected using the
 * Qualifier @NamedAuthProvider. For example; to inject a IdTokenValidator for
 * validating Apple ID-Tokens:
 *
 * <pre>
 *   \@Inject
 *   \@NamedAuthProvider(AuthProvider.APPLE)
 *   IdTokenValidator idTokenValidator;
 * </pre>
 */
@Slf4j
public class AppleFactory extends OpenIdFactory {
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
    public OpenIdTokenApi appleRestApi(@NamedAuthProvider(AuthProvider.APPLE) OpenIdConfiguration.AuthConfig authConfig,
                                       @NamedAuthProvider(AuthProvider.APPLE) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(authConfig, openIdConfig);
    }

    /**
     * Returns a resolver to retrieve the Json Web (Public) Keys used to validate
     * signed JWT tokens from Apple. The resolver will use the Json Web Key-Set
     * referenced by the URI found in Google's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public VerificationKeyResolver appleKeys(@NamedAuthProvider(AuthProvider.APPLE) OpenIdConfigResponse openIdConfig) {
        return verificationKeys(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the Apple
     * auth-provider. The validator will use the Json Web Key-Set referenced by the
     * URI found in Apple's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.APPLE)
    @Singleton
    public IdTokenValidator appleTokenValidator(@NamedAuthProvider(AuthProvider.APPLE) VerificationKeyResolver verificationKeys,
                                                @NamedAuthProvider(AuthProvider.APPLE) OpenIdConfiguration.AuthConfig config,
                                                @NamedAuthProvider(AuthProvider.APPLE) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(verificationKeys, config, openIdConfig);
    }
}
