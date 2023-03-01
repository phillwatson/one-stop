package com.hillayes.auth.xsrf;

import com.hillayes.auth.errors.EncryptionConfigException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
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

//@ApplicationScoped
@Slf4j
public class XsrfGenerator {
    private static final String SIGNATURE_ALG = "HmacSHA256";
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final Random random;
    private final Mac mac;

    /**
     * The name of the cookie, in the incoming request, that holds the XSRF token
     * to be compared with that held in the named http header.
     */
    @Getter @Setter
    private String cookieName = "XSRF-TOKEN";

    /**
     * The name of the http header, in the incoming request, that holds the XSRF
     * token to be compared with that held in the named request cookie.
     */
    @Getter @Setter
    private String headerName = "X-XSRF-TOKEN";

    /**
     * The duration for which the generated XSRF token is valid - in seconds.
     */
    @Getter @Setter
    private long timeoutSecs = Duration.ofMinutes(30).toSeconds();

    public XsrfGenerator(String secret) {
        log.info("Creating XSRF generator");
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
     * @param token1 one of the token pairs to be compared.
     * @param token2 the other of the token pairs to be compared.
     * @return true if the two tokens are valid and match.
     */
    public boolean validateTokens(String token1, String token2) {
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

        long timeoutMillis = timeoutSecs * 1000;
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

    /**
     * Provides an entry point for validating an XSRF token passed on the header
     * properties of the given javax.ws.rs HttpHeaders.
     *
     * @param headers the http request on which the XSRF token is expected.
     * @return true if the request contains a valid XSRF token.
     */
    public boolean validateToken(HttpHeaders headers) {
        log.trace("Validating XSRF tokens in request headers");

        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            log.trace("XSRF token cookie missing [name: {}]", cookieName);
            return false;
        }

        String cookieValue = cookie.getValue();
        if (isBlank(cookieValue)) {
            log.trace("XSRF token cookie blank [name: {}]", cookieName);
            return false;
        }

        List<String> headerList = headers.getRequestHeader(headerName);
        if (headerList.size() != 1) {
            log.trace("XSRF token header invalid [name: {}, size: {}]", headerName, headerList.size());
            return false;
        }

        String headerValue = headerList.get(0);
        if (isBlank(headerValue)) {
            log.trace("XSRF token header blank [name: {}]", headerName);
            return false;
        }

        return validateTokens(cookieValue, headerValue);
    }

    /**
     * Provides an entry point for validating an XSRF token passed on the header
     * properties of the given javax.ws.rs ContainerRequestContext.
     *
     * @param request the http request on which the XSRF token is expected.
     * @return true if the request contains a valid XSRF token.
     */
    public boolean validateToken(ContainerRequestContext request) {
        log.trace("Validating XSRF tokens in context request");

        Cookie cookie = request.getCookies().get(cookieName);
        if (cookie == null) {
            log.trace("XSRF token cookie missing [name: {}]", cookieName);
            return false;
        }

        String cookieValue = cookie.getValue();
        if (isBlank(cookieValue)) {
            log.trace("XSRF token cookie blank [name: {}]", cookieName);
            return false;
        }

        List<String> headerList = request.getHeaders().get(headerName);
        if ((headerList == null) || (headerList.size() != 1)) {
            log.trace("XSRF token header invalid [name: {}, size: {}]",
                headerName, (headerList == null) ? 0 : headerList.size());
            return false;
        }

        String headerValue = headerList.get(0);
        if (isBlank(headerValue)) {
            log.trace("XSRF token header blank [name: {}]", headerName);
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
