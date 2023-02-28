package com.hillayes.user.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Slf4j
public class XsrfHandler {
    private static final String SIGNATURE_ALG = "HmacSHA256";
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final Random random;
    private final Mac mac;

    @Getter
    @Setter
    private String cookieName = "XSRF-TOKEN";

    @Getter
    @Setter
    private String headerName = "X-XSRF-TOKEN";

    @Getter
    @Setter
    private long timeout = Duration.ofMinutes(30).toMillis();

    public XsrfHandler(String secret) {
        try {
            random = new Random();
            mac = Mac.getInstance(SIGNATURE_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALG));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateToken() {
        log.debug("Generating XSRF token");
        byte[] salt = new byte[32];
        random.nextBytes(salt);

        String saltPlusToken = BASE64_ENCODER.encodeToString(salt) + "." + System.currentTimeMillis();
        String signature = BASE64_ENCODER.encodeToString(mac.doFinal(saltPlusToken.getBytes(StandardCharsets.US_ASCII)));

        return saltPlusToken + "." + signature;
    }

    public boolean validateTokens(String token1, String token2) {
        log.debug("Validating XSRF token");

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

        if (System.currentTimeMillis() > timestamp + timeout) {
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

        log.debug("XSRF token validation passed");
        return true;
    }

    public boolean validateToken(HttpHeaders headers) {
        log.debug("Validating XSRF tokens in request headers");

        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            log.debug("XSRF token cookie missing [name: {}]", cookieName);
            return false;
        }

        String cookieValue = cookie.getValue();
        if (isBlank(cookieValue)) {
            log.debug("XSRF token cookie blank [name: {}]", cookieName);
            return false;
        }

        List<String> headerList = headers.getRequestHeader(headerName);
        if (headerList.size() != 1) {
            log.debug("XSRF token header invalid [name: {}, size: {}]", headerName, headerList.size());
            return false;
        }

        String headerValue = headerList.get(0);
        if (isBlank(headerValue)) {
            log.debug("XSRF token header blank [name: {}]", headerName);
            return false;
        }

        return validateTokens(cookieValue, headerValue);
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

    private static boolean isBlank(String str) {
        return (str == null) || (str.isBlank());
    }
}
