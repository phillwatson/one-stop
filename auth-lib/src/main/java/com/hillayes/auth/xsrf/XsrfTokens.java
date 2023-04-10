package com.hillayes.auth.xsrf;

import com.hillayes.auth.errors.EncryptionConfigException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

import static com.hillayes.commons.Strings.isBlank;

@ApplicationScoped
@Slf4j
public class XsrfTokens {
    private static final String SIGNATURE_ALG = "HmacSHA256";
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final Random random;
    private final Mac mac;

    public XsrfTokens(@ConfigProperty(name = "one-stop.auth.xsrf.secret") String secret) {
        log.debug("Creating XSRF generator");
        try {
            random = new Random();
            mac = Mac.getInstance(SIGNATURE_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALG));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new EncryptionConfigException(e);
        }
    }

    /**
     * Generate a new XSRF token based on the given configuration - notably the secret.
     *
     * @return the generated and signed XSRF token.
     */
    public String generateToken() {
        log.trace("Generating XSRF token");
        byte[] salt = new byte[32];
        random.nextBytes(salt);

        String saltPlusToken = BASE64_ENCODER.encodeToString(salt) + "." + System.currentTimeMillis();
        String signature = BASE64_ENCODER.encodeToString(mac.doFinal(saltPlusToken.getBytes(StandardCharsets.US_ASCII)));

        return saltPlusToken + "." + signature;
    }

    /**
     * Tests that the given XSRF tokens are valid and equal.
     *
     * @param token1      one of the token pairs to be compared.
     * @param token2      the other of the token pairs to be compared.
     * @param timeoutSecs The duration after which the generated XSRF token is deemed invalid - in seconds.
     *                    As the XSRF token is generated whenever a refresh-token is created, this
     *                    should be at least equal to the time-to-live duration of the refresh-token.
     * @return true if the two tokens are valid and match.
     */
    public boolean validateTokens(String token1, String token2, int timeoutSecs) {
        log.trace("Validating XSRF token");

        if ((isBlank(token1)) || (isBlank(token2))) {
            log.warn("XSRF token validation failed - token missing");
            return false;
        }

        String[] tokenParts = token1.split("\\.");
        if (tokenParts.length != 3) {
            log.warn("XSRF token validation failed - invalid format");
            return false;
        }

        long timestamp = parseLong(tokenParts[1]);
        if (timestamp == -1) {
            log.warn("XSRF token validation failed - invalid time check");
            return false;
        }

        long timeoutMillis = timeoutSecs * 1000L;
        if (System.currentTimeMillis() > timestamp + timeoutMillis) {
            log.warn("XSRF token validation failed - token expired");
            return false;
        }

        // compare values
        final byte[] tokenBytes = token1.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(tokenBytes, token2.getBytes(StandardCharsets.UTF_8))) {
            log.warn("XSRF token validation failed - no match");
            return false;
        }

        byte[] saltPlusToken = (tokenParts[0] + "." + tokenParts[1]).getBytes(StandardCharsets.US_ASCII);
        synchronized (mac) {
            saltPlusToken = mac.doFinal(saltPlusToken);
        }

        final byte[] signature = BASE64_ENCODER.encode(saltPlusToken);
        if (!MessageDigest.isEqual(signature, tokenParts[2].getBytes(StandardCharsets.US_ASCII))) {
            log.warn("XSRF token validation failed - signature does not match");
            return false;
        }

        log.trace("XSRF token validation passed");
        return true;
    }

    private static long parseLong(String str) {
        if (isBlank(str)) {
            return -1;
        }

        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
