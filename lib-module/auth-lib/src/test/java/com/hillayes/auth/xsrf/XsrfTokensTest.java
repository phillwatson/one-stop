package com.hillayes.auth.xsrf;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XsrfTokensTest {
    private final XsrfTokens fixture = new XsrfTokens(UUID.randomUUID().toString());

    @Test
    public void testValidation() {
        String token = fixture.generateToken();
        assertTrue(fixture.validateTokens(token, token, Duration.ofSeconds(2000)));
    }

    @Test
    public void testValidation_differentHander_sameSecret() {
        String token = new XsrfTokens("this is a secret").generateToken();
        assertTrue(new XsrfTokens("this is a secret").validateTokens(token, token, Duration.ofSeconds(2000)));
    }

    @Test
    public void testMissingTokens() {
        String token = fixture.generateToken();
        assertFalse(fixture.validateTokens(token, null, Duration.ofSeconds(2000)));
        assertFalse(fixture.validateTokens(token, "", Duration.ofSeconds(2000)));

        assertFalse(fixture.validateTokens(null, token, Duration.ofSeconds(2000)));
        assertFalse(fixture.validateTokens("", token, Duration.ofSeconds(2000)));

        assertFalse(fixture.validateTokens(null, null, Duration.ofSeconds(2000)));
        assertFalse(fixture.validateTokens("", "", Duration.ofSeconds(2000)));

        assertFalse(fixture.validateTokens(null, "", Duration.ofSeconds(2000)));
        assertFalse(fixture.validateTokens("", null, Duration.ofSeconds(2000)));
    }
}
