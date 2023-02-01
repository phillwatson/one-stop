package com.hillayes.user.service;

import com.hillayes.user.auth.PasswordCrypto;
import com.hillayes.user.domain.User;
import com.hillayes.user.events.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import io.smallrye.jwt.build.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import java.security.GeneralSecurityException;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "one-stop.jwt.refresh-token.duration-secs")
    long refreshDuration;

    private final UserRepository userRepository;
    private final PasswordCrypto passwordCrypto;
    private final UserEventSender userEventSender;

    public String[] login(String username, char[] password) {
        log.info("Auth [user: {}, issuer: {}]", username, issuer);

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

        String accessToken = Jwt
            .issuer(issuer)
            .upn(username)
            .claim("purpose", "access")
            .groups("user")
            .sign();
        String refreshToken = Jwt
            .issuer(issuer)
            .upn(username)
            .claim("purpose", "refresh")
            .expiresIn(refreshDuration)
            .sign();

        userEventSender.sendUserLogin(user);
        return new String[]{accessToken, refreshToken};
    }

    public String[] refresh(String jwt) {
        return null;
    }
}
