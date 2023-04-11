package com.hillayes.openid.apple;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.Duration;

/**
 * Provides the OpenIdAuth implementation for the Google auth-provider. The
 * instance will be initialised with the Open-ID configuration and IdTokenValidator
 * appropriate for Google.
 */
@ApplicationScoped
@NamedAuthProvider(AuthProvider.APPLE)
@Slf4j
public class AppleAuth implements OpenIdAuth {
    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdTokenApi openIdTokenApi;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdConfigResponse authConfig;

    @Inject
    ClientSecretGenerator clientSecretGenerator;

    private PrivateKey privateKey = null;
    private long secretExpires = 0;
    private String clientSecret = null;

    @Override
    public boolean isFor(AuthProvider authProvider) {
        return authProvider == AuthProvider.APPLE;
    }

    public JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException, GeneralSecurityException {
        log.debug("Exchanging auth code for tokens [authCode: {}]", authCode);
        TokenExchangeResponse response = openIdTokenApi.exchangeToken(
            "authorization_code",
            config.clientId(),
            getClientSecret(),
            authCode,
            config.redirectUri()
        );
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        return idTokenValidator.verify(response.idToken);
    }

    /**
     * Returns the client-secret for the Apple provider. This is a JWT signed with
     * the private key for the Apple developer account.
     * A new client-secret is generated every 179 days; or on application restart.
     */
    private String getClientSecret() throws GeneralSecurityException {
        if (secretExpires < System.currentTimeMillis()) {
            Duration expires = Duration.ofDays(179); // max is 6 months - use 1 day less to be safe
            secretExpires = System.currentTimeMillis() + expires.toMillis();

            String kid = config.keyId().orElseThrow(() -> new NullPointerException("Apple keyId is required"));
            String teamId = config.teamId().orElseThrow(() -> new NullPointerException("Apple teamId is required"));

            clientSecret = clientSecretGenerator.createClientSecret(
                getPrivateKey(), kid,
                teamId, config.clientId(), authConfig.issuer,
                expires, SignatureAlgorithm.ES256
            );
        }
        return clientSecret;
    }

    private PrivateKey getPrivateKey() throws GeneralSecurityException {
        if (privateKey == null) {
            privateKey = clientSecretGenerator.decodePrivateKey(config.privateKey().get(), SignatureAlgorithm.ES256);
        }
        return privateKey;
    }

    public String toString() {
        return "OpenIdAuth["+ config.configUri() + "]";
    }
}
