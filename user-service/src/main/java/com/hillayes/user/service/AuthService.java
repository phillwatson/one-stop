package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.RotatedJwkSet;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.openid.OpenIdAuthentication;
import com.hillayes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.ws.rs.NotAuthorizedException;
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

    @Transactional
    public User oauthLogin(AuthProvider authProvider,
                           String code,
                           String state,
                           String scope) {
        try {
            log.info("OAuth login [provider: {}, code: {}, state: {}, scope: {}]", authProvider, code, state, scope);
            User user = openIdAuth.oauthLogin(authProvider, code);

            boolean newUser = (user.getId() == null);
            user = userRepository.save(user);

            if (newUser) {
                userEventSender.sendUserCreated(user);
            }

            userEventSender.sendUserLogin(user);
            log.debug("User tokens created [userId: {}]", user.getId());
            return user;
        } catch (Exception e) {
            log.error("Failed to verify OpenId auth-code.", e);
            userEventSender.sendLoginFailed(code, "Invalid open-id auth-code.");
            throw new NotAuthorizedException("jwt");
        }
    }

    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public User refresh(JsonWebToken jsonWebToken) {
        log.info("Auth refresh tokens initiated");

        UUID userId = UUID.fromString(jsonWebToken.getName());
        User user = userRepository.findById(userId)
            .filter(u -> !u.isBlocked())
            .orElseThrow(() -> {
                log.info("User name failed verification [userId: {}]", userId);
                userEventSender.sendLoginFailed(userId.toString(), "User not found.");
                return new NotAuthorizedException("username/password");
            });

        log.debug("User tokens refreshed [userId: {}]", user.getId());
        return user;
    }
}
