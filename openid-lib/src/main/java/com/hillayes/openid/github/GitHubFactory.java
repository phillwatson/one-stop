package com.hillayes.openid.github;

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
 * validating GitHub ID-Tokens:
 *
 * <pre>
 *   \@Inject
 *   \@NamedAuthProvider(AuthProvider.GITHUB)
 *   IdTokenValidator idTokenValidator;
 * </pre>
 */
@Slf4j
public class GitHubFactory extends OpenIdFactory {
    /**
     * Provides the open-id configuration for the GitHub auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITHUB)
    @Singleton
    public OpenIdConfiguration.AuthConfig googleConfig() {
        log.debug("Retrieving GitHub OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.GITHUB);
    }

    /**
     * Returns the open-id configuration from GitHub's "well-known" configuration URL,
     * taken from the given local configuration.
     * <p>
     * See https://accounts.google.com/.well-known/openid-configuration
     *
     * @param config the local configuration for GitHub auth-provider.
     * @return GitHub's own configuration properties.
     * @throws IOException if the auth-provider's configuration cannot be read.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITHUB)
    @Singleton
    public OpenIdConfigResponse googleOpenApiConfig(@NamedAuthProvider(AuthProvider.GITHUB) OpenIdConfiguration.AuthConfig config) throws IOException {
        return openApiConfig(config);
    }

    /**
     * Returns a REST client for the GitHub's auth-token exchange API. The URI of
     * this API endpoint is taken from the given open-id configuration, read from
     * GitHub's own "well-known" configuration.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITHUB)
    @Singleton
    public OpenIdTokenApi googleRestApi(@NamedAuthProvider(AuthProvider.GITHUB) OpenIdConfiguration.AuthConfig authConfig,
                                        @NamedAuthProvider(AuthProvider.GITHUB) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(authConfig, openIdConfig);
    }

    /**
     * Returns a resolver to retrieve the Json Web (Public) Keys used to validate
     * signed JWT tokens from GitHub. The resolver will use the Json Web Key-Set
     * referenced by the URI found in GitHub's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITHUB)
    @Singleton
    public VerificationKeyResolver googleKeys(@NamedAuthProvider(AuthProvider.GITHUB) OpenIdConfigResponse openIdConfig) {
        return verificationKeys(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the GitHub
     * auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITHUB)
    @Singleton
    public IdTokenValidator googleTokenValidator(@NamedAuthProvider(AuthProvider.GITHUB) VerificationKeyResolver verificationKeys,
                                                 @NamedAuthProvider(AuthProvider.GITHUB) OpenIdConfiguration.AuthConfig config,
                                                 @NamedAuthProvider(AuthProvider.GITHUB) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(verificationKeys, config, openIdConfig);
    }
}