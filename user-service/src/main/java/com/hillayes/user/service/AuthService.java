package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.RotatedJwkSet;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.openid.OpenIdAuthentication;
import com.hillayes.user.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;

import java.net.URI;
import java.util.UUID;

/**
 * refresh duration 30 minutes - governs how long user session can be inactive
 * access duration 5 minutes - governs how often user account is checked (deleted/blocked)
 * key rotation 30 minutes - must be no less than refresh duration
 */
@Singleton
@Slf4j
public class AuthService {
    @Inject
    UserRepository userRepository;
    @Inject
    PasswordCrypto passwordCrypto;
    @Inject
    UserEventSender userEventSender;
    @Inject
    OpenIdAuthentication openIdAuth;
    @Inject
    RotatedJwkSet jwkSet;

    /**
     * Provides external access to the public keys used to verify signed auth
     * tokens. These can be cached by the caller for a configured duration.
     * This allows other services within the architecture to verify the auth
     * tokens without having to rely upon the user-service.
     *
     * @return the public key set used to verify signed JWT auth tokens.
     */
    public String getJwkSet() {
        return jwkSet.toJson();
    }

    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public User login(String username, char[] password) {
        log.info("Auth login initiated");

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.info("User name failed verification");
                userEventSender.sendLoginFailed(username, "User not found.");
                return new NotAuthorizedException("username/password");
            });

        if (user.isBlocked()) {
            log.info("User login failed [id: {}, blocked: {}]", user.getId(), user.isBlocked());
            userEventSender.sendLoginFailed(username, "User blocked or deleted.");
            throw new NotAuthorizedException("username/password");
        }

        if (!passwordCrypto.verify(password, user.getPasswordHash())) {
            log.info("User password failed verification");
            userEventSender.sendLoginFailed(username, "Invalid password.");
            throw new NotAuthorizedException("username/password");
        }

        userEventSender.sendUserLogin(user);
        log.debug("User logged in [userId: {}]", user.getId());
        return user;
    }

    public URI oauthLogin(AuthProvider authProvider,
                          String state) {
        log.info("OAuth login [provider: {}, state: {}]", authProvider, state);
        return openIdAuth.oauthLogin(authProvider, state);
    }

    @Transactional
    public User oauthValidate(AuthProvider authProvider,
                              String code,
                              String state,
                              String scope) {
        try {
            log.info("OAuth validate [provider: {}, code: {}, state: {}, scope: {}]", authProvider, code, state, scope);
            User user = openIdAuth.oauthExchange(authProvider, code);

            boolean newUser = (user.getId() == null);
            user = userRepository.save(user);

            if (newUser) {
                userEventSender.sendUserCreated(user);
            }

            userEventSender.sendUserLogin(user);
            log.debug("User tokens created [userId: {}]", user.getId());
            return user;
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify OpenId auth-code.", e);
            userEventSender.sendLoginFailed(code, "Invalid open-id auth-code.");
            throw new NotAuthorizedException("jwt");
        }
    }

    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public User refresh(JsonWebToken jsonWebToken) {
        log.info("Auth refresh tokens initiated");

        if (jsonWebToken == null) {
            log.info("No refresh token cookie found.");
            throw new NotAuthorizedException("JWT");
        }

        UUID userId = UUID.fromString(jsonWebToken.getName());
        User user = userRepository.findByIdOptional(userId)
            .filter(u -> !u.isBlocked())
            .orElseThrow(() -> {
                log.info("User name failed verification [userId: {}]", userId);
                userEventSender.sendLoginFailed(userId.toString(), "User not found.");
                return new NotAuthorizedException("JWT");
            });

        log.debug("User tokens refreshed [userId: {}]", user.getId());
        return user;
    }
}
