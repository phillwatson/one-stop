package com.hillayes.user.openid;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import java.util.Map;
import java.util.Optional;

/**
 * A configuration record for a supported Auth-Provider. Supplies the
 * registered properties used to identify the application when communicating
 * with the Auth-Provider.
 * <p>
 * See OpenIdProvider for producer methods to inject configurations
 * required for a given Auth Provider.
 */
@ConfigMapping(prefix = "one-stop.auth.openid")
public interface OpenIdConfiguration {
    /**
     * Returns a Map of the internal configuration for those Open-ID providers
     * to which the application has been registered. Each configuration is keyed
     * on the AuthProvider enum to which it relates.
     */
    @WithParentName()
    Map<AuthProvider, AuthConfig> configs();

    /**
     * The internal configuration for accessing the API of an Open-ID provider
     * to which this application has been registered.
     */
    interface AuthConfig {
        /**
         * The Auth-Provider's OpenID "well-known" uri, from which other configuration
         * properties can be obtained.
         * e.g. 'https://accounts.google.com/.well-known/openid-configuration'
         */
        String configUri();

        /**
         * The unique identifier assigned to our application by the Auth-Provider.
         * Used to verify communications with that Auth-Provider.
         */
        String clientId();

        /**
         * The unique secret assigned to our application by the Auth-Provider.
         * Used to verify communications with that Auth-Provider.
         */
        Optional<String> clientSecret();

        /**
         * The URI passed to the Auth-Provider when authentication is initiated by
         * the client. The Auth-Provider will invoke this URI when user authentication
         * is complete. The same URI will then be passed back to the Auth-Provider
         * when a request is made to exchange the Auth-Provider's auth-token for the
         * authenticated user's details.
         */
        String redirectUri();
    }
}
