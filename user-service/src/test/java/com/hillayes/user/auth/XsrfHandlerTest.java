package com.hillayes.user.auth;

import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XsrfHandlerTest {
    private XsrfHandler fixture = new XsrfHandler(UUID.randomUUID().toString());

    @Test
    public void testValidation() {
        String token = fixture.generateToken();
        assertTrue(fixture.validateTokens(token, token));
    }

    @Test
    public void testValidation_differentHander_sameSecret() {
        String token = new XsrfHandler("this is a secret").generateToken();
        assertTrue(new XsrfHandler("this is a secret").validateTokens(token, token));
    }

    @Test
    public void testMissingTokens() {
        String token = fixture.generateToken();
        assertFalse(fixture.validateTokens(token, null));
        assertFalse(fixture.validateTokens(token, ""));

        assertFalse(fixture.validateTokens(null, token));
        assertFalse(fixture.validateTokens("", token));

        assertFalse(fixture.validateTokens(null, null));
        assertFalse(fixture.validateTokens("", ""));

        assertFalse(fixture.validateTokens(null, ""));
        assertFalse(fixture.validateTokens("", null));
    }

    @Test
    public void testValidationHeaders() {
        String token = fixture.generateToken();

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token)));
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token));

        assertTrue(fixture.validateToken(headers));
    }

    @Test
    public void testMissingCookie() {
        String token = fixture.generateToken();

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of());
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token));

        assertFalse(fixture.validateToken(headers));
    }

    @Test
    public void testTooManyHeaders() {
        String token = fixture.generateToken();

        HttpHeaders headers = mock();
        Map<String, Cookie> mockCookies = Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token));
        when(headers.getCookies()).thenReturn(mockCookies);
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token, "second-value"));

        assertFalse(fixture.validateToken(headers));
    }

    @Test
    public void testMissingHeader() {
        String token = fixture.generateToken();

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token)));
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of());

        assertFalse(fixture.validateToken(headers));
    }

    @Test
    public void testTimeout() throws InterruptedException {
        fixture.setTimeout(1000);
        String token = fixture.generateToken();
        synchronized (token) {
            token.wait(1000);
        }

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token)));
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token));

        assertFalse(fixture.validateToken(headers));
    }

    @Test
    public void testTokenMismatch_sameHandler() {
        String token1 = fixture.generateToken();
        String token2 = fixture.generateToken();

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token1)));
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token2));

        assertFalse(fixture.validateToken(headers));
    }

    @Test
    public void testTokenMismatch_differentHandler_differentSecret() {
        String token1 = fixture.generateToken();
        String token2 = new XsrfHandler("this is a different secret").generateToken();

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token1)));
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token2));

        assertFalse(fixture.validateToken(headers));
    }

    @Test
    public void testTokenMismatch_differentHandler_sameSecret() {
        String token1 = new XsrfHandler("this is a secret").generateToken();
        String token2 = new XsrfHandler("this is a secret").generateToken();

        HttpHeaders headers = mock();
        when(headers.getCookies()).thenReturn(Map.of(fixture.getCookieName(), new Cookie(fixture.getCookieName(), token1)));
        when(headers.getRequestHeader(fixture.getHeaderName())).thenReturn(List.of(token2));

        assertFalse(fixture.validateToken(headers));
    }
}
