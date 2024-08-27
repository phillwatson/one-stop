package com.hillayes.openid.linkedin;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

import java.io.IOException;

/**
 * A collection of Producer/Factory methods to inject and configure the OpenID
 * auth provider implementations. Each configuration can be injected using the
 * Qualifier @NamedAuthProvider. For example; to inject a IdTokenValidator for
 * validating LinkedIn ID-Tokens:
 *
 * <pre>
 *   \@Inject
 *   \@NamedAuthProvider(AuthProvider.LINKEDIN)
 *   IdTokenValidator idTokenValidator;
 * </pre>
 */
@Slf4j
public class LinkedInFactory extends OpenIdFactory {
    /**
     * Provides the open-id configuration for the LinkedIn auth-provider.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.LINKEDIN)
    @ApplicationScoped
    public OpenIdConfiguration.AuthConfig linkedinConfig() {
        log.debug("Retrieving LinkedIn OpenId Config");
        return openIdConfiguration.configs().get(AuthProvider.LINKEDIN);
    }

    /**
     * Returns the open-id configuration from LinkedIn's "well-known" configuration URL,
     * taken from the given local configuration.
     * <p>
     * See https://www.linkedin.com/oauth/.well-known/openid-configuration
     *
     * @param config the local configuration for LinkedIn auth-provider.
     * @return LinkedIn's own configuration properties.
     * @throws IOException if the auth-provider's configuration cannot be read.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.LINKEDIN)
    public OpenIdConfigResponse linkedinOpenApiConfig(@NamedAuthProvider(AuthProvider.LINKEDIN) OpenIdConfiguration.AuthConfig config) throws IOException {
        return openApiConfig(config);
    }

    /**
     * Returns a REST client for the LinkedIn's auth-token exchange API. The URI of
     * this API endpoint is taken from the given open-id configuration, read from
     * LinkedIn's own "well-known" configuration.
     */
    @Produces
    @ApplicationScoped
    @NamedAuthProvider(AuthProvider.LINKEDIN)
    public OpenIdTokenApi linkedinRestApi(@NamedAuthProvider(AuthProvider.LINKEDIN) OpenIdConfigResponse openIdConfig) {
        return openIdRestApi(openIdConfig);
    }

    /**
     * Returns a resolver to retrieve the Json Web (Public) Keys used to validate
     * signed JWT tokens from LinkedIn. The resolver will use the Json Web Key-Set
     * referenced by the URI found in LinkedIn's "well-known" config resource.
     */
    @Produces
    @NamedAuthProvider(AuthProvider.LINKEDIN)
    public VerificationKeyResolver linkedinKeys(@NamedAuthProvider(AuthProvider.LINKEDIN) OpenIdConfigResponse openIdConfig) {
        return verificationKeys(openIdConfig);
    }

    /**
     * Provides the IdTokenValidator instance for validating ID-Tokens from the LinkedIn
     * auth-provider.
     */
    @Produces
    @ApplicationScoped
    @NamedAuthProvider(AuthProvider.LINKEDIN)
    public IdTokenValidator linkedinTokenValidator(@NamedAuthProvider(AuthProvider.LINKEDIN) VerificationKeyResolver verificationKeys,
                                                   @NamedAuthProvider(AuthProvider.LINKEDIN) OpenIdConfiguration.AuthConfig config,
                                                   @NamedAuthProvider(AuthProvider.LINKEDIN) OpenIdConfigResponse openIdConfig) {
        return idTokenValidator(verificationKeys, config, openIdConfig);
    }
}
