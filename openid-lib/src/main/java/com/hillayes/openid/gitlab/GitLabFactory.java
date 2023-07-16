package com.hillayes.openid.gitlab;

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
 * validating GitLab ID-Tokens:
 *
 * <pre>
 *   \@Inject
 *   \@NamedAuthProvider(AuthProvider.GITLAB)
 *   IdTokenValidator idTokenValidator;
 * </pre>
 */
@Slf4j
public class GitLabFactory extends OpenIdFactory {
    /**
     * Provides the open-id configuration for the GitLab auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITLAB)
    @Singleton
    public OpenIdConfiguration.AuthConfig gitlabConfig() {
        log.debug("Retrieving GitLab OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.GITLAB);
    }

    /**
     * Returns the open-id configuration from GitLab's "well-known" configuration URL,
     * taken from the given local configuration.
     * <p>
     * See https://gitlab.com/.well-known/openid-configuration
     *
     * @param config the local configuration for GitLab auth-provider.
     * @return GitLab's own configuration properties.
     * @throws IOException if the auth-provider's configuration cannot be read.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITLAB)
    @Singleton
    public OpenIdConfigResponse gitlabOpenApiConfig(@NamedAuthProvider(AuthProvider.GITLAB) OpenIdConfiguration.AuthConfig config) throws IOException {
        return openApiConfig(config);
    }

    /**
     * Returns a REST client for the GitLab's auth-token exchange API. The URI of
     * this API endpoint is taken from the given open-id configuration, read from
     * GitLab's own "well-known" configuration.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITLAB)
    @Singleton
    public OpenIdTokenApi gitlabRestApi(@NamedAuthProvider(AuthProvider.GITLAB) OpenIdConfiguration.AuthConfig authConfig,
                                        @NamedAuthProvider(AuthProvider.GITLAB) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(authConfig, openIdConfig);
    }

    /**
     * Returns a resolver to retrieve the Json Web (Public) Keys used to validate
     * signed JWT tokens from GitLab. The resolver will use the Json Web Key-Set
     * referenced by the URI found in GitLab's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITLAB)
    @Singleton
    public VerificationKeyResolver gitlabKeys(@NamedAuthProvider(AuthProvider.GITLAB) OpenIdConfigResponse openIdConfig) {
        return verificationKeys(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the GitLab
     * auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.GITLAB)
    @Singleton
    public IdTokenValidator gitlabTokenValidator(@NamedAuthProvider(AuthProvider.GITLAB) VerificationKeyResolver verificationKeys,
                                                 @NamedAuthProvider(AuthProvider.GITLAB) OpenIdConfiguration.AuthConfig config,
                                                 @NamedAuthProvider(AuthProvider.GITLAB) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(verificationKeys, config, openIdConfig);
    }
}
