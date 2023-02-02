package com.hillayes.user.service;

import com.hillayes.user.auth.PasswordCrypto;
import com.hillayes.user.domain.User;
import com.hillayes.user.events.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import java.security.GeneralSecurityException;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "one-stop.jwt.access-token.duration-secs")
    long accessDuration;

    @ConfigProperty(name = "one-stop.jwt.refresh-token.duration-secs")
    long refreshDuration;

    private final UserRepository userRepository;
    private final PasswordCrypto passwordCrypto;
    private final UserEventSender userEventSender;
    private final JWTParser jwtParser;

    public String[] login(String username, char[] password) {
        log.info("Auth login initiated [issuer: {}]", issuer);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.info("User name failed verification");
                userEventSender.sendLoginFailed("username", "User not found.");
                return new NotAuthorizedException("username/password");
            });

        try {
            if (!passwordCrypto.verify(password, user.getPasswordHash())) {
                log.info("User password failed verification");
                userEventSender.sendLoginFailed("username", "Invalid password.");
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

    public String[] refresh(String jwt) {
        try {
            log.info("Auth refresh tokens initiated [issuer: {}]", issuer);

            JsonWebToken jsonWebToken = jwtParser.parse(jwt);

            UUID userId = UUID.fromString(jsonWebToken.getClaim(Claims.upn));
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.info("User name failed verification [userId: {}]", userId);
                    userEventSender.sendLoginFailed("username", "User not found.");
                    return new NotAuthorizedException("username/password");
                });

            String[] tokens = buildTokens(user);
            log.debug("User tokens refreshed [userId: {}]", user.getId());
            return tokens;
        } catch (ParseException e) {
            log.error("Failed to verify refresh token.", e);
            throw new NotAuthorizedException("username/password");
        }
    }

    private String[] buildTokens(User user) {
        String accessToken = Jwt
            .issuer(issuer)
            .upn(user.getId().toString())
            .subject(user.getUsername())
            .claim("purpose", "access")
            .expiresIn(accessDuration)
            .groups("user")
            .sign();
        String refreshToken = Jwt
            .issuer(issuer)
            .upn(user.getId().toString())
            .subject(user.getUsername())
            .claim("purpose", "refresh")
            .expiresIn(refreshDuration)
            .sign();
        return new String[]{accessToken, refreshToken};
    }
}
