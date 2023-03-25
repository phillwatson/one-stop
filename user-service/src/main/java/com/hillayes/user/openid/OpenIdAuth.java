package com.hillayes.user.openid;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.user.domain.User;
import com.hillayes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import java.util.Set;
import java.util.UUID;

/**
 * Provides implementations to perform Auth-Code Flow authentication.
 * This abstract class provides the code common to all implementations, and
 * subclasses must provide the interaction with the auth-provider's API.
 */
@Slf4j
public abstract class OpenIdAuth {
    @Inject
    UserRepository userRepository;

    @Inject
    PasswordCrypto passwordCrypto;

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
     *
     *
     */
    public abstract boolean isFor(AuthProvider authProvider);

    /**
     * Implements must call the auth-provider's auth-code verification endpoint to
     * exchange the given auth-code for access-token, refresh-token and the user's
     * ID-token (a JWT containing information pertaining to the authenticated user).
     *
     * @param authCode the auth-code to be verified.
     * @return the JWT claims from the authenticated user's ID-Token.
     * @throws InvalidJwtException if the obtained ID-token is not valid.
     */
    public abstract JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException;

    public User oauthLogin(String authCode) {
        try {
            log.info("OAuth login");

            JwtClaims idToken = exchangeAuthToken(authCode);
            if (log.isTraceEnabled()) {
                idToken.getClaimNames().forEach(name -> log.trace("ID Token [{}: {}]", name, idToken.getClaimValue(name)));
            }

            String issuer = idToken.getIssuer();
            String subject = idToken.getSubject();
            String email = idToken.getClaimValueAsString("email");

            // lookup user by Auth Provider's Identity
            User user = userRepository.findByIssuerAndSubject(issuer, subject)
                .orElse(null);

            // if found
            if (user != null) {
                log.debug("Found user by OpenID subject [issuer: {}, subject: {}]", issuer, subject);

                // if Auth Provider's Identity is disabled
                user.getOidcIdentity(issuer)
                    .filter(oidc -> !oidc.isDisabled())
                    .orElseThrow(() -> {
                        log.debug("User's OpenID is disabled");
                        return new NotAuthorizedException("jwt");
                    });
            } else {
                log.debug("Did not find user by OpenID subject [issuer: {}, subject: {}]", issuer, subject);

                // look-up user by email from Auth Provider
                user = userRepository.findByEmail(email).stream()
                    .filter(existing -> !existing.isDeleted())
                    .findFirst()
                    .map(u -> {
                        log.debug("Found user by OpenID email [userId: {}, email: {}]", u.getId(), email);
                        if (u.isBlocked()) {
                            log.debug("User is blocked [userId: {}]", u.getId());
                            throw new NotAuthorizedException("OpenId");
                        }
                        return u;
                    })
                    // if deleted OR not found - create a new User
                    .orElseGet(() -> {
                        String name = idToken.getClaimValueAsString("name");
                        String givenName = idToken.getClaimValueAsString("given_name");
                        String familyName = idToken.getClaimValueAsString("family_name");
                        return User.builder()
                                .username(email)
                                .passwordHash(passwordCrypto.getHash(UUID.randomUUID().toString().toCharArray()))
                                .email(email)
                                .givenName(givenName == null ? name == null ? email : name : givenName)
                                .familyName(familyName)
                                .roles(Set.of("user"))
                                .build();
                        }
                    );

                // record Auth Provider Identity against user
                user.addOidcIdentity(issuer, subject);
            }

            if (user.getId() == null) {
                log.debug("User logged in via OpenID [created new user]");
            } else {
                log.debug("User logged in via OpenID [userId: {}]", user.getId());
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to verify refresh token.", e);
            throw new NotAuthorizedException("jwt");
        }
    }
}
