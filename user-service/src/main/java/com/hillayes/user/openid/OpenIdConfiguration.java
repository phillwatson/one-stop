package com.hillayes.user.openid;

import io.smallrye.config.ConfigMapping;

/**
 * A configuration record for a supported Auth-Provider. Supplies the
 * registered properties used to identify the application when communicating
 * with the Auth-Provider.
 */
public interface OpenIdConfiguration {
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
    String clientSecret();

    /**
     * The URI passed to the Auth-Provider when authentication is initiated by
     * the client. The Auth-Provider will invoke this URI when user authentication
     * is complete. The same URI will then be passed back to the Auth-Provider
     * when a request is made to exchange the Auth-Provider's auth-token for the
     * authenticated user's details.
     */
    String redirectUri();
}
