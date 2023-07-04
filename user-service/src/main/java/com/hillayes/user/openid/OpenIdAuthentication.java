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
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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

    public User oauthLogin(AuthProvider authProvider, String authCode) {
        try {
            log.info("OAuth login");

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

            // if no email was provided by Auth Provider
            else if (email == null) {
                throw new NotAuthorizedException("jwt");
            }

            // look-up user by their email
            else {
                log.debug("Did not find user by OpenID subject [issuer: {}, subject: {}]", issuer, subject);

                // look-up user by email from Auth Provider
                user = userRepository.findByEmail(email)
                    .map(u -> {
                        log.debug("Found user by OpenID email [userId: {}, email: {}]", u.getId(), email);
                        if (u.isBlocked()) {
                            log.error("User is blocked [userId: {}]", u.getId());
                            throw new NotAuthorizedException("OpenId");
                        }
                        return u;
                    })
                    // if not found - create a new User
                    .orElseGet(() -> {
                        String name = idToken.getClaimValueAsString("name");
                        String givenName = idToken.getClaimValueAsString("given_name");
                        String familyName = idToken.getClaimValueAsString("family_name");
                        String locale = idToken.getClaimValueAsString("locale");
                        return User.builder()
                                .username(email)
                                .passwordHash(passwordCrypto.getHash(UUID.randomUUID().toString().toCharArray()))
                                .email(email)
                                .givenName(givenName == null ? name == null ? email : name : givenName)
                                .familyName(familyName)
                                .preferredName(name == null ? givenName == null ? email : givenName : name)
                                .dateOnboarded(Instant.now())
                                .locale(locale == null ? null : Locale.forLanguageTag(locale))
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
            log.error("Failed to verify auth token.", e);
            throw new NotAuthorizedException("jwt");
        }
    }
}
