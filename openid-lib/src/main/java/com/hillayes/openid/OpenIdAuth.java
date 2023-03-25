package com.hillayes.openid;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

/**
 * Provides implementations to perform Auth-Code Flow authentication.
 * This abstract class provides the code common to all implementations, and
 * subclasses must provide the interaction with the auth-provider's API.
 */
public interface OpenIdAuth {
    /**
     * Used to select the OpenIdAuth implementation based on the given AuthProvider
     * value. We could inject each implementation explicitly, but that would require
     * additional work when new implementations are introduced. Instead, we inject
     * all instances using the javax.enterprise.inject.Instance<OpenIdAuth>, and use
     * this method to identify the appropriate instance. For example;
     * <pre>
     *     \@Inject \@Any
     *     Instance<OpenIdAuth> openIdAuths;
     * </pre>
     * See OpenIdAuthTest for more examples.
     *
     * @param authProvider the AuthProvider value that identifies the implementation.
     * @return true if this instance supports the given AuthProvider.
     */
    boolean isFor(AuthProvider authProvider);

    /**
     * Implements must call the auth-provider's auth-code verification endpoint to
     * exchange the given auth-code for access-token, refresh-token and the user's
     * ID-token (a JWT containing information pertaining to the authenticated user).
     *
     * @param authCode the auth-code to be verified.
     * @return the JWT claims from the authenticated user's ID-Token.
     * @throws InvalidJwtException if the obtained ID-token is not valid.
     */
    JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException;
}
