package com.hillayes.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.RotatedJwkSet;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.openid.OpenIdAuthentication;
import com.hillayes.user.repository.UserRepository;
import io.smallrye.jwt.auth.cdi.NullJsonWebToken;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class AuthServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordCrypto passwordCrypto;
    @Mock
    UserEventSender userEventSender;
    @Mock
    OpenIdAuthentication openIdAuth;
    @Mock
    RotatedJwkSet jwkSet;

    @InjectMocks
    AuthService fixture;

    @BeforeEach
    public void beforeEach() {
        openMocks(this);

        // simulate setting of ID when record is persisted
        when(userRepository.save(any())).then((invocation) -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
    }

    @Test
    public void testGetJwkSet() throws JsonProcessingException {
        // given: the JWK set is configured
        when(jwkSet.toJson()).thenReturn("{\"keys\":[{\"kty\":\"RSA\"}]}");

        // when: we request the JWK set
        String json = fixture.getJwkSet();

        // then: a JSON payload is returned
        assertNotNull(json);

        // and: the payload looks like a key-set
        JsonNode node = new ObjectMapper().reader().readTree(json);
        assertNotNull(node);
        assertEquals("RSA", node.get("keys").get(0).get("kty").asText());
    }

    @ParameterizedTest
    @EnumSource(AuthProvider.class)
    public void testInitiateOpenIdLogin(AuthProvider authProvider) {
        // given: some state to pass open-id provider
        String state = insecure().nextAlphanumeric(20);

        // and: the open-id auth-provider will give a redirect URI
        URI redirectUri = URI.create("http://mock-uri");
        when(openIdAuth.oauthLogin(authProvider, state)).thenReturn(redirectUri);

        // when: the service is called
        URI uri = fixture.oauthLogin(authProvider, state);

        // then: the open-id login is initiated
        verify(openIdAuth).oauthLogin(authProvider, state);

        // and: the response URI is correct
        assertEquals(redirectUri, uri);
    }

    @Test
    public void testLogin_HappyPath() {
        // given: a user signing in with their username and password
        String username = insecure().nextAlphanumeric(20);
        char[] password = insecure().nextAlphanumeric(20).toCharArray();

        // and: the user record exists
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(username)
            .passwordHash(insecure().nextAlphanumeric(20))
            .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // and: the password is correct
        when(passwordCrypto.verify(password, user.getPasswordHash())).thenReturn(true);

        // when: the service is called
        fixture.login(username, password);

        // then: the login is recorded
        verify(userEventSender).sendUserAuthenticated(user, null);
    }

    @Test
    public void testLogin_UserNotFound() {
        // given: a user signing in with their username and password
        String username = insecure().nextAlphanumeric(20);
        char[] password = insecure().nextAlphanumeric(20).toCharArray();

        // and: the user record does NOT exist
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.login(username, password));

        // then: the no login is recorded
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());

        // and: the login failure is recorded
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendAuthenticationFailed(eq(username), any(), captor.capture());

        // and: the failure reason is recorded
        assertEquals("User not found.", captor.getValue());
    }

    @Test
    public void testLogin_PasswordInvalid() {
        // given: a user signing in with their username and password
        String username = insecure().nextAlphanumeric(20);
        char[] password = insecure().nextAlphanumeric(20).toCharArray();

        // and: the user record exists - but is blocked
        User user = User.builder()
            .username(username)
            .passwordHash(insecure().nextAlphanumeric(20))
            .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // and: the password is incorrect
        when(passwordCrypto.verify(password, user.getPasswordHash())).thenReturn(false);

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.login(username, password));

        // then: the no login is recorded
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());

        // and: the login failure is recorded
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendAuthenticationFailed(eq(username), any(), captor.capture());

        // and: the failure reason is recorded
        assertEquals("Invalid password.", captor.getValue());
    }

    @Test
    public void testLogin_UserIsBlocked() {
        // given: a user signing in with their username and password
        String username = insecure().nextAlphanumeric(20);
        char[] password = insecure().nextAlphanumeric(20).toCharArray();

        // and: the user record exists - but is blocked
        User user = User.builder()
            .username(username)
            .passwordHash(insecure().nextAlphanumeric(20))
            .dateBlocked(Instant.now())
            .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // and: the password is incorrect
        when(passwordCrypto.verify(password, user.getPasswordHash())).thenReturn(false);

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.login(username, password));

        // then: the no login is recorded
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());

        // and: the login failure is recorded
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendAuthenticationFailed(eq(username), any(), captor.capture());

        // and: the failure reason is recorded
        assertEquals("User blocked or deleted.", captor.getValue());
    }

    @Test
    public void testOauthLogin_HappyPath_ExistingUser() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = insecure().nextAlphanumeric(30);
        String state = insecure().nextAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: an existing user is authenticated
        User user = User.builder()
            .id(UUID.randomUUID())
            .build();
        when(openIdAuth.oauthExchange(authProvider, code)).thenReturn(user);

        // when: the service is called
        User response = fixture.oauthValidate(authProvider, code, state, scope);

        // then: the user record is returned
        assertEquals(user, response);

        // and: NO new user creation is recorded
        verify(userEventSender, never()).sendUserCreated(any());

        // and: the login is recorded
        verify(userEventSender).sendUserAuthenticated(user, authProvider);
    }

    @Test
    public void testOauthLogin_HappyPath_NewUser() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = insecure().nextAlphanumeric(30);
        String state = insecure().nextAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: a new user is authenticated
        User user = User.builder()
            .build();
        when(openIdAuth.oauthExchange(authProvider, code)).thenReturn(user);

        // when: the service is called
        User response = fixture.oauthValidate(authProvider, code, state, scope);

        // then: the user record is returned
        assertEquals(user, response);

        // and: the user is saved
        verify(userRepository).save(user);

        // and: the new user creation is recorded
        verify(userEventSender).sendUserCreated(user);

        // and: the login is recorded
        verify(userEventSender).sendUserAuthenticated(user, authProvider);
    }

    @Test
    public void testOauthLogin_InvalidAuthCode() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = insecure().nextAlphanumeric(30);
        String state = insecure().nextAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: a new user is authenticated
        User user = User.builder()
            .build();
        when(openIdAuth.oauthExchange(authProvider, code)).thenThrow(new RuntimeException("some error"));

        // when: the service is called
        // then: an auth-error is raised
        assertThrows(NotAuthorizedException.class, () -> fixture.oauthValidate(authProvider, code, state, scope));

        // and: NO user is saved
        verify(userRepository, never()).save(user);

        // and: NO new user creation is recorded
        verify(userEventSender, never()).sendUserCreated(user);

        // and: NO login is recorded
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());

        // and: a login-failure is recorded
        verify(userEventSender).sendAuthenticationFailed(eq(code), eq(authProvider), any());
    }

    @Test
    public void testOauthLogin_AuthFails() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = insecure().nextAlphanumeric(30);
        String state = insecure().nextAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: auth provider fails to authenticate
        when(openIdAuth.oauthExchange(authProvider, code))
            .thenThrow(new NotAuthorizedException("OpenId"));

        // when: the service is called
        assertThrows(NotAuthorizedException.class,
            () -> fixture.oauthValidate(authProvider, code, state, scope));

        // then: NO user is saved
        verify(userRepository, never()).save(any());

        // and: NO new user creation is recorded
        verify(userEventSender, never()).sendUserCreated(any());

        // and: NO login is recorded
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());
    }

    @Test
    public void testRefresh_HappyPath() {
        // given: a user wishing to refresh their auth tokens
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(insecure().nextAlphanumeric(20))
            .passwordHash(insecure().nextAlphanumeric(20))
            .build();
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a refresh token identifying that user
        JsonWebToken refreshToken = new NullJsonWebToken() {
            public String getName() {
                return user.getId().toString();
            }
        };

        // when the service is called
        User response = fixture.refresh(refreshToken);

        // then: the identified user is returned
        assertEquals(user, response);
    }

    @Test
    public void testRefresh_UserNotFound() {
        // given: an unknown user wishing to refresh their auth tokens
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

        // and: a refresh token identifying that user
        JsonWebToken refreshToken = new NullJsonWebToken() {
            public String getName() {
                return userId.toString();
            }
        };

        // when the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.refresh(refreshToken));
    }

    @Test
    public void testRefresh_UserIsBlocked() {
        // given: a user wishing to refresh their auth tokens
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(insecure().nextAlphanumeric(20))
            .passwordHash(insecure().nextAlphanumeric(20))
            .dateBlocked(Instant.now())
            .build();
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a refresh token identifying that user
        JsonWebToken refreshToken = new NullJsonWebToken() {
            public String getName() {
                return user.getId().toString();
            }
        };

        // when the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.refresh(refreshToken));
    }
}
