package com.hillayes.user.openid;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.commons.Strings;
import com.hillayes.openid.AuthProvider;
import com.hillayes.openid.OpenIdAuth;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Provides implementations to perform Auth-Code Flow authentication.
 * This abstract class provides the code common to all implementations, and
 * subclasses must provide the interaction with the auth-provider's API.
 */
@ApplicationScoped
@Slf4j
public class OpenIdAuthentication {
    @Inject
    UserRepository userRepository;

    @Inject
    PasswordCrypto passwordCrypto;

    @Inject @Any
    Instance<OpenIdAuth> openIdAuths;

    @Inject
    UserEventSender userEventSender;

    /**
     * Locates the OpenIdAuthentication instance that can handle authentication for the
     * identified AuthProvider.
     * @param authProvider the AuthProvider value that identifies the implementation.
     * @return the identified OpenIdAuthentication provider.
     */
    private OpenIdAuth getOpenIdAuth(AuthProvider authProvider) {
        return openIdAuths.stream()
            .filter(instance -> instance.isFor(authProvider))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("AuthProvider not implemented:  " + authProvider));
    }

    public URI oauthLogin(AuthProvider authProvider, String state) {
        log.info("OAuth login [authProvider: {}, state: {}]", authProvider, state);

        URI redirect = getOpenIdAuth(authProvider).initiateLogin(state);
        log.info("OAuth login [authProvider: {}, redirect: {}]", authProvider, redirect);
        return redirect;
    }

    /**
     * Exchanges, with the identifed AuthProvider, the given auth-code for an ID-Token and
     * uses the ID-Token to authenticate the user.
     * <p>
     * Each user holds is associated with one or more OpenID Connect identities; by their
     * issuer and subject. If the ID-Token identifies an existing user, that user is returned.
     * <p>
     * Otherwise, an attempt is made to locate the user by their email address. If found, the
     * user is updated with the new OpenID Connect identity and returned.
     * <p>
     * If no existing user can be found (by OpenID Connect identity or email address), a new
     * user is created. The username of that user will be derived from the ID-Token's claims.
     * <p>
     * The email address is the first choice for the username, but if not provided, the ID token
     * claims are searched for a "login" or "username". If none of these are found, or if a
     * user already exists with that username, the "subject" of the ID token is used; the subject
     * being the Auth Provider's unique identifier for the user.
     *
     * @param authProvider the AuthProvider that the authCode was obtained from.
     * @param authCode the auth-code to be exchanged with the AuthProvider.
     * @return the user authenticated by the ID-Token.
     * @throws NotAuthorizedException if the user cannot be found, or created, or is not
     * authorized to access the system.
     */
    public User oauthExchange(AuthProvider authProvider, String authCode) throws NotAuthorizedException {
        try {
            log.info("OAuth exchange [authProvider: {}, authCode: {}]", authProvider, authCode);

            JwtClaims idToken = getOpenIdAuth(authProvider).exchangeAuthToken(authCode);
            if (log.isTraceEnabled()) {
                idToken.getClaimNames().forEach(name -> log.trace("ID Token [{}: {}]", name, idToken.getClaimValue(name)));
            }

            String issuer = idToken.getIssuer();
            String subject = idToken.getSubject();
            String val = idToken.getClaimValueAsString("email");
            String email = Strings.isBlank(val) ? null : val.toLowerCase();

            // lookup user by Auth Provider's Identity
            User user = userRepository.findByIssuerAndSubject(issuer, subject)
                .orElse(null);

            // if found
            if (user != null) {
                log.debug("Found user by OpenID subject [issuer: {}, subject: {}]", issuer, subject);

                if (user.isBlocked()) {
                    throw new NotAuthorizedException("OpenId");
                }

                // if Auth Provider's Identity is disabled
                user.getOidcIdentity(issuer)
                    .filter(oidc -> !oidc.isDisabled())
                    .orElseThrow(() -> {
                        log.debug("User's OpenID is disabled");
                        return new NotAuthorizedException("OpenId");
                    });

                // take opportunity to update user's email address
                if ((email != null) && (! email.equals(user.getEmail()))) {
                    user.setEmail(email);
                    userEventSender.sendUserUpdated(user);
                }
            }

            // no existing user found for Auth Provider's Identity
            else {
                log.debug("Did not find user by OpenID subject [issuer: {}, subject: {}]", issuer, subject);

                // look-up (or create) user by email from Auth Provider
                user = findOrCreateUser(email, idToken);

                // record Auth Provider Identity against user
                user.addOidcIdentity(authProvider, issuer, subject);
            }

            if (user.getId() == null) {
                log.debug("User logged in via OpenID [created new user]");
            } else {
                log.debug("User logged in via OpenID [userId: {}]", user.getId());
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to verify auth token.", e);
            throw new NotAuthorizedException("jwt");
        }
    }

    /**
     * Locates the user with the given email address or, if not found, creates a new user
     * from the properties of the given open-id profile.
     *
     * @param email email address from open-id profile.
     * @param idToken the open-id ID-Token containing user profile.
     * @return the user identified by the email address, or a new user created from ID-Token
     */
    private User findOrCreateUser(String email, JwtClaims idToken) {
        // look-up user by email from Auth Provider
        return userRepository.findByEmail(email)
            .map(user -> {
                log.debug("Found user by OpenID email [userId: {}, email: {}]", user.getId(), Strings.maskEmail(email));
                if (user.isBlocked()) {
                    log.error("User is blocked [userId: {}]", user.getId());
                    throw new NotAuthorizedException("OpenId");
                }
                return user;
            })
            // if not found - create a new User
            .orElseGet(() -> {
                String name = idToken.getClaimValueAsString("name");
                String givenName = idToken.getClaimValueAsString("given_name");
                String familyName = idToken.getClaimValueAsString("family_name");
                String locale = idToken.getClaimValueAsString("locale");

                // if no email was provided by Auth Provider
                String username = (email != null) ? email
                    : selectUserName(idToken, idToken.getClaimValueAsString("sub"));

                return User.builder()
                    .username(username)
                    .passwordHash(null)
                    .email(email)
                    .givenName(givenName == null ? name == null ? email : name : givenName)
                    .familyName(familyName)
                    .preferredName(name == null ? givenName == null ? email : givenName : name)
                    .dateOnboarded(Instant.now())
                    .locale(locale == null ? null : Locale.forLanguageTag(locale))
                    .roles(Set.of("user"))
                    .build();
            });
    }

    /**
     * The ordered list of ID-Token claims from which a username can be derived. Any
     * claim that contains these values in their name are considered.
     */
    private static final String[] POSSIBLE_USERNAME_CLAIMS = {"login", "username"};

    /**
     * Attempts to locate a username from the given ID-Token. The username is taken from
     * given default value.
     *
     * @param idToken the ID-Token from which to extract the username.
     * @param defaultValue the default value to use if no username can be found.
     * @return the username from the ID-Token, or the default value if none can be found.
     */
    private String selectUserName(JwtClaims idToken, String defaultValue) {
        Collection<String> claimNames = idToken.getClaimNames();
        return Arrays.stream(POSSIBLE_USERNAME_CLAIMS)
            .map(contender -> claimNames.stream()
                .map(String::toLowerCase)
                .filter(claimName -> claimName.contains(contender))
                .map(idToken::getClaimValueAsString)
                .filter(username -> userRepository.findByUsername(username).isEmpty())
                .findFirst()
                .orElse(null)
            )
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(defaultValue);
    }
}
