package com.hillayes.user.openid;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.user.domain.User;
import com.hillayes.user.events.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import java.util.Objects;
import java.util.Set;

@Slf4j
public abstract class OpenIdAuth {
    @Inject
    UserRepository userRepository;

    @Inject
    PasswordCrypto passwordCrypto;

    @Inject
    UserEventSender userEventSender;

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
            String email = idToken.getClaimValue("email", String.class);

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

                // take the opportunity to update the email address
                if (!Objects.equals(user.getEmail(), email)) {
                    user.setEmail(email);
                    userEventSender.sendUserUpdated(user);
                }
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
                    .orElse(User.builder()
                        .username(email)
                        .passwordHash(passwordCrypto.getHash("password".toCharArray()))
                        .email(email)
                        .givenName("")
                        .roles(Set.of("user"))
                        .build()
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
