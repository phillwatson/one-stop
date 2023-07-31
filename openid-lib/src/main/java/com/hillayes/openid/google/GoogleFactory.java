package com.hillayes.openid.google;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

import java.io.IOException;

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
public class GoogleFactory extends OpenIdFactory {
    /**
     * Provides the open-id configuration for the Google auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
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
    public OpenIdTokenApi googleRestApi(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig authConfig,
                                        @NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(authConfig, openIdConfig);
    }

    /**
     * Returns a resolver to retrieve the Json Web (Public) Keys used to validate
     * signed JWT tokens from Google. The resolver will use the Json Web Key-Set
     * referenced by the URI found in Google's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    public VerificationKeyResolver googleKeys(@NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfigResponse openIdConfig) {
        return verificationKeys(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the Google
     * auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GOOGLE)
    public IdTokenValidator googleTokenValidator(@NamedAuthProvider(AuthProvider.GOOGLE) VerificationKeyResolver verificationKeys,
                                                 @NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfiguration.AuthConfig config,
                                                 @NamedAuthProvider(AuthProvider.GOOGLE) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(verificationKeys, config, openIdConfig);
    }
}
