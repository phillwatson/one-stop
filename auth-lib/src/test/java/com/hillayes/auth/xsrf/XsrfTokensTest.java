package com.hillayes.auth.xsrf;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XsrfTokensTest {
    private final XsrfTokens fixture = new XsrfTokens(UUID.randomUUID().toString());

    @Test
    public void testValidation() {
        String token = fixture.generateToken();
        assertTrue(fixture.validateTokens(token, token, 2000));
    }

    @Test
    public void testValidation_differentHander_sameSecret() {
        String token = new XsrfTokens("this is a secret").generateToken();
        assertTrue(new XsrfTokens("this is a secret").validateTokens(token, token, 2000));
    }

    @Test
    public void testMissingTokens() {
        String token = fixture.generateToken();
        assertFalse(fixture.validateTokens(token, null, 2000));
        assertFalse(fixture.validateTokens(token, "", 2000));

        assertFalse(fixture.validateTokens(null, token, 2000));
        assertFalse(fixture.validateTokens("", token, 2000));

        assertFalse(fixture.validateTokens(null, null, 2000));
        assertFalse(fixture.validateTokens("", "", 2000));

        assertFalse(fixture.validateTokens(null, "", 2000));
        assertFalse(fixture.validateTokens("", null, 2000));
    }
}
