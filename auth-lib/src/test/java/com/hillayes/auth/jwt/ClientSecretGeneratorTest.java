package com.hillayes.auth.jwt;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.Duration;

public class ClientSecretGeneratorTest {
    private ClientSecretGenerator fixture = new ClientSecretGenerator();

    @Test
    public void test() throws GeneralSecurityException, IOException {
        // aka - bundle ID
        SignatureAlgorithm ALGORITHM = SignatureAlgorithm.ES256;
        String CLIENT_ID = "velopaymentsHibanaPayQA";
        String TEAM_ID = "S5474NHS5U";
        String KEY_ID = "69V4Q9N572";

        PrivateKey privateKey = fixture.readPrivateKey("apple-id.p8", ALGORITHM);

        String signedToken = fixture.createSignedToken(privateKey,
            KEY_ID, TEAM_ID, CLIENT_ID, "https://appleid.apple.com",
            Duration.ofHours(15), ALGORITHM);
        System.out.println(signedToken);
    }
}
