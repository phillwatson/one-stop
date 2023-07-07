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
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.jwt.auth.cdi.NullJsonWebToken;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AuthServiceTest {
    @InjectMock
    UserRepository userRepository;
    @InjectMock
    PasswordCrypto passwordCrypto;
    @InjectMock
    UserEventSender userEventSender;
    @InjectMock
    OpenIdAuthentication openIdAuth;
    @Inject
    RotatedJwkSet jwkSet;

    @Inject
    ObjectMapper jsonMapper;

    @Inject
    AuthService fixture;

    @BeforeEach
    public void beforeEach() {
        // simulate setting of ID when record is persisted
        when(userRepository.save(any())).then((invocation) -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user = user.toBuilder().id(UUID.randomUUID()).build();
            }
            return user;
        });
    }

    @Test
    public void testGetJwkSet() throws JsonProcessingException {
        // when: we request the JWK set
        String json = fixture.getJwkSet();

        // then: a JSON payload is returned
        assertNotNull(json);

        // and: the payload looks like a key-set
        JsonNode node = jsonMapper.reader().readTree(json);
        assertNotNull(node);
        assertEquals("RSA", node.get("keys").get(0).get("kty").asText());
    }

    @Test
    public void testLogin_HappyPath() {
        // given: a user signing in with their username and password
        String username = randomAlphanumeric(20);
        char[] password = randomAlphanumeric(20).toCharArray();

        // and: the user record exists
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(username)
            .passwordHash(randomAlphanumeric(20))
            .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // and: the password is correct
        when(passwordCrypto.verify(password, user.getPasswordHash())).thenReturn(true);

        // when: the service is called
        fixture.login(username, password);

        // then: the login is recorded
        verify(userEventSender).sendUserLogin(user);
    }

    @Test
    public void testLogin_UserNotFound() {
        // given: a user signing in with their username and password
        String username = randomAlphanumeric(20);
        char[] password = randomAlphanumeric(20).toCharArray();

        // and: the user record does NOT exist
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.login(username, password));

        // then: the no login is recorded
        verify(userEventSender, never()).sendUserLogin(any());

        // and: the login failure is recorded
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendLoginFailed(eq(username), captor.capture());

        // and: the failure reason is recorded
        assertEquals("User not found.", captor.getValue());
    }

    @Test
    public void testLogin_PasswordInvalid() {
        // given: a user signing in with their username and password
        String username = randomAlphanumeric(20);
        char[] password = randomAlphanumeric(20).toCharArray();

        // and: the user record exists - but is blocked
        User user = User.builder()
            .username(username)
            .passwordHash(randomAlphanumeric(20))
            .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // and: the password is incorrect
        when(passwordCrypto.verify(password, user.getPasswordHash())).thenReturn(false);

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.login(username, password));

        // then: the no login is recorded
        verify(userEventSender, never()).sendUserLogin(any());

        // and: the login failure is recorded
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendLoginFailed(eq(username), captor.capture());

        // and: the failure reason is recorded
        assertEquals("Invalid password.", captor.getValue());
    }

    @Test
    public void testLogin_UserIsBlocked() {
        // given: a user signing in with their username and password
        String username = randomAlphanumeric(20);
        char[] password = randomAlphanumeric(20).toCharArray();

        // and: the user record exists - but is blocked
        User user = User.builder()
            .username(username)
            .passwordHash(randomAlphanumeric(20))
            .dateBlocked(Instant.now())
            .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // and: the password is incorrect
        when(passwordCrypto.verify(password, user.getPasswordHash())).thenReturn(false);

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.login(username, password));

        // then: the no login is recorded
        verify(userEventSender, never()).sendUserLogin(any());

        // and: the login failure is recorded
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendLoginFailed(eq(username), captor.capture());

        // and: the failure reason is recorded
        assertEquals("User blocked or deleted.", captor.getValue());
    }

    @Test
    public void testOauthLogin_HappyPath_ExistingUser() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = randomAlphanumeric(30);
        String state = randomAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: an existing user is authenticated
        User user = User.builder()
            .id(UUID.randomUUID())
            .build();
        when(openIdAuth.oauthLogin(authProvider, code)).thenReturn(user);

        // when: the service is called
        User response = fixture.oauthLogin(authProvider, code, state, scope);

        // then: the user record is returned
        assertEquals(user, response);

        // and: NO new user creation is recorded
        verify(userEventSender, never()).sendUserCreated(any());

        // and: the login is recorded
        verify(userEventSender).sendUserLogin(user);
    }

    @Test
    public void testOauthLogin_HappyPath_NewUser() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = randomAlphanumeric(30);
        String state = randomAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: a new user is authenticated
        User user = User.builder()
            .build();
        when(openIdAuth.oauthLogin(authProvider, code)).thenReturn(user);

        // when: the service is called
        User response = fixture.oauthLogin(authProvider, code, state, scope);

        // then: the user record is returned
        assertEquals(user, response);

        // and: the user is saved
        verify(userRepository).save(user);

        // and: the new user creation is recorded
        verify(userEventSender).sendUserCreated(user);

        // and: the login is recorded
        verify(userEventSender).sendUserLogin(user);
    }

    @Test
    public void testOauthLogin_InvalidAuthCode() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = randomAlphanumeric(30);
        String state = randomAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: a new user is authenticated
        User user = User.builder()
            .build();
        when(openIdAuth.oauthLogin(authProvider, code)).thenThrow(new RuntimeException("some error"));

        // when: the service is called
        // then: an auth-error is raised
        assertThrows(NotAuthorizedException.class, () -> fixture.oauthLogin(authProvider, code, state, scope));

        // and: NO user is saved
        verify(userRepository, never()).save(user);

        // and: NO new user creation is recorded
        verify(userEventSender, never()).sendUserCreated(user);

        // and: NO login is recorded
        verify(userEventSender, never()).sendUserLogin(user);

        // and: a login-failure is recorded
        verify(userEventSender).sendLoginFailed(eq(code), any());
    }

    @Test
    public void testOauthLogin_AuthFails() {
        // given: an identified auth provider
        AuthProvider authProvider = AuthProvider.GOOGLE;

        // and: the auth data provided by that provider
        String code = randomAlphanumeric(30);
        String state = randomAlphanumeric(10);
        String scope = "openid,profile,email";

        // and: auth provider fails to authenticate
        when(openIdAuth.oauthLogin(authProvider, code))
            .thenThrow(new NotAuthorizedException("OpenId"));

        // when: the service is called
        assertThrows(NotAuthorizedException.class,
            () -> fixture.oauthLogin(authProvider, code, state, scope));

        // then: NO user is saved
        verify(userRepository, never()).save(any());

        // and: NO new user creation is recorded
        verify(userEventSender, never()).sendUserCreated(any());

        // and: NO login is recorded
        verify(userEventSender, never()).sendUserLogin(any());
    }

    @Test
    public void testRefresh_HappyPath() {
        // given: a user wishing to refresh their auth tokens
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(20))
            .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

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
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

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
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(20))
            .dateBlocked(Instant.now())
            .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

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