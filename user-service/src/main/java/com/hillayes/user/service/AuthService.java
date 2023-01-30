package com.hillayes.user.service;

import com.hillayes.user.auth.PasswordCrypto;
import com.hillayes.user.repository.UserRepository;
import io.smallrye.jwt.build.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    @ConfigProperty(name="mp.jwt.verify.issuer")
    String issuer;

    private final UserRepository userRepository;
    private final PasswordCrypto passwordCrypto;

    public String login(String username, char[] password) {
        log.info("Auth issuer: {}", issuer);
        return Jwt.issuer(issuer)
                .upn(username)
                .groups("user")
                .sign();
    }
}
