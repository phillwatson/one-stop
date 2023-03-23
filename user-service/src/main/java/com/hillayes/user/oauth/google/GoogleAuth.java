package com.hillayes.user.oauth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.domain.User;
import com.hillayes.user.model.OpenIdConfigResponse;
import com.hillayes.user.model.TokenExchangeRequest;
import com.hillayes.user.model.TokenExchangeResponse;
import com.hillayes.user.repository.GoogleAuthRepository;
import com.hillayes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jose4j.jwt.JwtClaims;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

@ApplicationScoped
@Slf4j
public class GoogleAuth {
    @ConfigProperty(name = "one-stop.auth.google.openid-config")
    String openidConfigUrl;

    @ConfigProperty(name = "one-stop.auth.google.client-id")
    String clientId;

    @ConfigProperty(name = "one-stop.auth.google.client-secret")
    String clientSecret;

    @ConfigProperty(name = "one-stop.auth.google.redirect-uri")
    String redirectUri;

    @Inject
    @RestClient
    GoogleAuthRepository googleAuthRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordCrypto passwordCrypto;

    @Inject
    ObjectMapper mapper;

    private JwtValidator jwtValidator;
    private OpenIdConfigResponse openIdConfig;

    @PostConstruct
    public void init() throws IOException {
        log.info("Retrieving Google OpenId Config [url: {}]", openidConfigUrl);
        URL url = new URL(openidConfigUrl);
        openIdConfig = mapper.readValue(url, OpenIdConfigResponse.class);

        log.info("Using Google ID key-set [url: {}]", openIdConfig.jwksUri);
        jwtValidator = new JwtValidator(openIdConfig.jwksUri, openIdConfig.issuer, clientId);
    }

    public User oauthLogin(String authCode) {
        try {
            log.info("OAuth login");

            TokenExchangeRequest request = TokenExchangeRequest.builder()
                .grantType("authorization_code")
                .redirectUri(redirectUri)
                .code(authCode)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
            TokenExchangeResponse response = googleAuthRepository.exchangeToken(request);

            log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

            JwtClaims idToken = jwtValidator.verify(response.idToken);
            if (log.isTraceEnabled()) {
                idToken.getClaimNames().forEach(name -> log.trace("ID Token [{}: {}]", name, idToken.getClaimValue(name)));
            }

            String issuer = idToken.getIssuer();
            String subject = idToken.getSubject();
            String email = idToken.getClaimValue("email", String.class);

            // lookup user by Auth Provider's Identity
            log.debug("Looking for user by GoogleID [issuer: {}, subject: {}]", issuer, subject);
            User user = userRepository.findByIssuerAndSubject(issuer, subject)
                .orElse(null);

            // if found
            if (user != null) {
                log.debug("Found user by GoogleID [issuer: {}, subject: {}]", issuer, subject);

                // if Auth Provider's Identity is disabled
                user.getOidcIdentity(issuer)
                    .filter(oidc -> !oidc.isDisabled())
                    .orElseThrow(() -> {
                        log.debug("User's OpenID is disabled");
                        return new NotAuthorizedException("jwt");
                    });

                // take the opportunity to update the email address
                user.setEmail(email);
            } else {
                log.debug("Did not find user by GoogleID [issuer: {}, subject: {}]", issuer, subject);

                // look-up user by email from Auth Provider
                user = userRepository.findByEmail(email).stream()
                    .filter(existing -> !existing.isDeleted())
                    .findFirst()
                    .map(u -> {
                        log.debug("Found user by Google email [userId: {}, email: {}]", u.getId(), email);
                        if (u.isBlocked()) {
                            log.debug("User is blocked [userId: {}]", u.getId());
                            throw new NotAuthorizedException("jwt");
                        }
                        return u;
                    })
                    // if deleted or not found - create a new User
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
                log.debug("User logged in via GoogleID [created new user]");
            } else {
                log.debug("User logged in via GoogleID [userId: {}]", user.getId());
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to verify refresh token.", e);
            throw new NotAuthorizedException("jwt");
        }
    }
}
