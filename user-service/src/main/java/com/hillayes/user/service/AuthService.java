package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.RotatedJwkSet;
import com.hillayes.user.domain.User;
import com.hillayes.user.events.UserEventSender;
import com.hillayes.user.oauth.AuthProvider;
import com.hillayes.user.oauth.google.GoogleAuth;
import com.hillayes.user.repository.UserRepository;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthProtocolState;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * refresh duration 30 minutes - governs how long user session can be inactive
 * access duration 5 minutes - governs how often user account is checked (deleted/blocked)
 * key rotation 30 minutes - must be no less than refresh duration
 */
@Singleton
@Slf4j
public class AuthService {
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "mp.jwt.verify.audiences")
    String audiencesList;

    private Set<String> audiences;

    @ConfigProperty(name = "one-stop.jwt.access-token.duration-secs")
    long accessDuration;

    @ConfigProperty(name = "one-stop.jwt.refresh-token.duration-secs")
    long refreshDuration;

    @ConfigProperty(name = "one-stop.jwk.set-size", defaultValue = "2")
    int jwkSetSize;

    @Inject
    UserRepository userRepository;
    @Inject
    PasswordCrypto passwordCrypto;
    @Inject
    UserEventSender userEventSender;
    @Inject
    JWTParser jwtParser;

    @Inject
    GoogleAuth googleAuth;

    private RotatedJwkSet jwkSet;

    @PostConstruct
    void init() {
        jwkSet = new RotatedJwkSet(jwkSetSize, refreshDuration);

        // audiences config prop is a comma-delimited list - we need a Set
        audiences = Arrays.stream(audiencesList.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    @PreDestroy
    void destroy() {
        jwkSet.destroy();
    }

    public String getJwkSet() {
        return jwkSet.toJson();
    }

    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public String[] login(String username, char[] password) {
        log.info("Auth login initiated [issuer: {}]", issuer);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.info("User name failed verification");
                userEventSender.sendLoginFailed(username, "User not found.");
                return new NotAuthorizedException("username/password");
            });

        if ((user.isDeleted()) || (user.isBlocked())) {
            log.info("User login failed [id: {}, deleted: {}, blocked: {}]",
                user.getId(), user.isDeleted(), user.isBlocked());
            userEventSender.sendLoginFailed(username, "User blocked or deleted.");
            throw new NotAuthorizedException("username/password");
        }

        try {
            if (!passwordCrypto.verify(password, user.getPasswordHash())) {
                log.info("User password failed verification");
                userEventSender.sendLoginFailed(username, "Invalid password.");
                throw new NotAuthorizedException("username/password");
            }
        } catch (GeneralSecurityException e) {
            log.error("Failed to verify password", e);
            throw new InternalServerErrorException(e);
        }

        String[] tokens = buildTokens(user);
        userEventSender.sendUserLogin(user);
        log.debug("User logged in [userId: {}]", user.getId());
        return tokens;
    }

    @Transactional
    public String[] oauthLogin(AuthProvider authProvider,
                               String code,
                               String state,
                               String scope) {
        try {
            log.info("OAuth login [provider: {}, code: {}, state: {}, scope: {}]", authProvider, code, state, scope);
            User user = googleAuth.oauthLogin(code);

            boolean newUser = (user.getId() == null);
            user = userRepository.save(user);

            if (newUser) {
                userEventSender.sendUserCreated(user);
                userEventSender.sendUserOnboarded(user);
            } else {
                userEventSender.sendUserUpdated();
            }

            String[] tokens = buildTokens(user);
            userEventSender.sendUserLogin(user);
            log.debug("User tokens created [userId: {}]", user.getId());
            return tokens;
        } catch (Exception e) {
            log.error("Failed to verify OpenId auth-code.", e);
            userEventSender.sendLoginFailed(code, "Invalid open-id auth-code.");
            throw new NotAuthorizedException("jwt");
        }
    }

    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public String[] refresh(String jwt) {
        try {
            log.info("Auth refresh tokens initiated [issuer: {}]", issuer);

            JsonWebToken jsonWebToken = jwtParser.parse(jwt);

            UUID userId = UUID.fromString(jsonWebToken.getClaim(Claims.upn));
            User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted() && !u.isBlocked())
                .orElseThrow(() -> {
                    log.info("User name failed verification [userId: {}]", userId);
                    userEventSender.sendLoginFailed(userId.toString(), "User not found.");
                    return new NotAuthorizedException("username/password");
                });

            String[] tokens = buildTokens(user);
            log.debug("User tokens refreshed [userId: {}]", user.getId());
            return tokens;
        } catch (ParseException e) {
            log.error("Failed to verify refresh token.", e);
            throw new InternalServerErrorException(e);
        }
    }

    private String[] buildTokens(User user) {
        String accessToken = jwkSet.signClaims(Jwt
            .issuer(issuer)
            .audience(audiences)
            .upn(user.getId().toString()) // this will be the Principal of the security context
            .expiresIn(accessDuration)
            .groups(user.getRoles()));

        String refreshToken = jwkSet.signClaims(Jwt
            .issuer(issuer)
            .audience(audiences)
            .upn(user.getId().toString()) // this will be the Principal of the security context
            .expiresIn(refreshDuration));

        return new String[]{accessToken, refreshToken};
    }
}
