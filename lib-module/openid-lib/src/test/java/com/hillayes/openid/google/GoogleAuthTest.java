package com.hillayes.openid.google;

import com.hillayes.openid.*;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.mockito.MockitoConfig;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.inject.Inject;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class GoogleAuthTest {
    @InjectMock
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdTokenApi openIdTokenApi;

    @InjectMock
    @NamedAuthProvider(AuthProvider.GOOGLE)
    IdTokenValidator idTokenValidator;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdAuth fixture;

    @Test
    public void testIsFor() {
        assertTrue(fixture.isFor(AuthProvider.GOOGLE));
    }

    @Test
    public void testExchangeAuthToken() throws InvalidJwtException, GeneralSecurityException {
        // given a token-exchange request and response
        ArgumentCaptor<TokenExchangeRequest> tokenExchangeCaptor = ArgumentCaptor.forClass(TokenExchangeRequest.class);
        TokenExchangeResponse tokenExchangeResponse = mockTokenExchangeResponse();
        when(openIdTokenApi.exchangeToken(tokenExchangeCaptor.capture())).thenReturn(tokenExchangeResponse);

        // and a valid id-token in that response
        JwtClaims claims = new JwtClaims();
        when(idTokenValidator.verify(anyString())).thenReturn(claims);

        // when we call the Google auth implementation
        JwtClaims idToken = fixture.exchangeAuthToken("mock-auth-code");
        assertNotNull(idToken);

        // then the token API is called with the token-exchange request
        verify(openIdTokenApi).exchangeToken(any(TokenExchangeRequest.class));

        // and the token exchange request is correct
        TokenExchangeRequest response = tokenExchangeCaptor.getValue();
        assertEquals("authorization_code", response.grantType);
        assertEquals("mock-auth-code", response.code);
        assertEquals(config.clientId().get(), response.clientId);
        assertEquals(config.clientSecret().get(), response.clientSecret);
        assertEquals(config.redirectUri(), response.redirectUri);
    }

    private TokenExchangeResponse mockTokenExchangeResponse() {
        TokenExchangeResponse result = new TokenExchangeResponse();
        result.accessToken = UUID.randomUUID().toString();
        result.refreshToken = UUID.randomUUID().toString();
        result.idToken = "mock-id-token";
        result.expiresIn = Duration.ofSeconds(3600).getSeconds();
        return result;
    }
}
