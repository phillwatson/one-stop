package com.hillayes.openid.apple;

import com.hillayes.openid.*;
import com.hillayes.openid.AuthProvider;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import com.hillayes.openid.rest.TokenExchangeRequest;
import com.hillayes.openid.rest.TokenExchangeResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.inject.Inject;
import java.security.*;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AppleAuthTest {
    @InjectMock(convertScopes = true)
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdTokenApi openIdTokenApi;

    @InjectMock(convertScopes = true)
    @NamedAuthProvider(AuthProvider.APPLE)
    IdTokenValidator idTokenValidator;

    @InjectMock
    ClientSecretGenerator clientSecretGenerator;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdConfigResponse providerConfig;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdAuth fixture;

    @Test
    public void testIsFor() {
        assertTrue(fixture.isFor(AuthProvider.APPLE));
    }

    @Test
    public void testExchangeAuthToken() throws InvalidJwtException, GeneralSecurityException {
        PrivateKey privateKey = createKeyPair().getPrivate();
        when(clientSecretGenerator.decodePrivateKey(any(), eq(SignatureAlgorithm.ES256)))
            .thenReturn(privateKey);
        when(clientSecretGenerator.createClientSecret(any(), any(), any(), any(), any(), any(), eq(SignatureAlgorithm.ES256)))
            .thenReturn("mock-client-secret");

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
        TokenExchangeRequest request = tokenExchangeCaptor.getValue();
        assertEquals("authorization_code", request.grantType);
        assertEquals("mock-auth-code", request.code);
        assertEquals(config.clientId(), request.clientId);
        assertEquals("mock-client-secret", request.clientSecret);
        assertEquals(config.redirectUri(), request.redirectUri);

        // and the client secret is generated correctly
        verify(clientSecretGenerator).decodePrivateKey(eq(config.privateKey().get()), eq(SignatureAlgorithm.ES256));
        verify(clientSecretGenerator).createClientSecret(
            eq(privateKey),
            eq(config.keyId().get()),
            eq(config.teamId().get()),
            eq(config.clientId()),
            eq(providerConfig.issuer),
            any(), eq(SignatureAlgorithm.ES256)
        );
    }

    private TokenExchangeResponse mockTokenExchangeResponse() {
        TokenExchangeResponse result = new TokenExchangeResponse();
        result.accessToken = UUID.randomUUID().toString();
        result.refreshToken = UUID.randomUUID().toString();
        result.idToken = "mock-id-token";
        result.expiresIn = Duration.ofSeconds(3600).getSeconds();
        return result;
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
