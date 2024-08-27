package com.hillayes.openid.apple;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ClientSecretGeneratorTest {
    private static final String TEAM_ID = "my-apple-team-id";
    private static final String CLIENT_ID = "my-apple-client-id";
    private static final String AUDIENCE = "https://appleid.apple.com";
    private static final String KEY_ID = "69V4Q9N572";
    private static final SignatureAlgorithm algorithm = SignatureAlgorithm.ES256;

    private final ClientSecretGenerator fixture = new ClientSecretGenerator();

    @Test
    public void test() throws GeneralSecurityException, InvalidJwtException, MalformedClaimException {
        KeyPair keyPair = createKeyPair();

        String clientSecret = fixture.createClientSecret(
            keyPair.getPrivate(), KEY_ID,
            TEAM_ID, CLIENT_ID, AUDIENCE,
            Duration.ofSeconds(1000),
            algorithm
        );
        assertNotNull(clientSecret);

        // Verify the given JWT token using the public key.
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setAllowedClockSkewInSeconds(30)
            .setRequireSubject()
            .setExpectedAudience(AUDIENCE)
            .setExpectedIssuer(TEAM_ID)
            .setVerificationKey(keyPair.getPublic())
            .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s)
                org.jose4j.jwa.AlgorithmConstraints.ConstraintType.PERMIT, algorithm.name()
            )
            .build();

        JwtClaims claims = jwtConsumer.processToClaims(clientSecret);
        assertEquals(CLIENT_ID, claims.getSubject());
    }

    /**
     * Generate a KeyPair using elliptic curve cryptography
     */
    private KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        return keyGen.generateKeyPair();
    }
}
