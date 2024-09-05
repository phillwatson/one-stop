package com.hillayes.user.resource;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.onestop.api.LoginRequest;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.openid.OpenIdAuthentication;
import com.hillayes.user.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.mockito.MockitoConfig;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AuthResourceTest extends TestBase {
    @ConfigProperty(name = "one-stop.auth.xsrf.cookie")
    String xsrfCookieName;
    @ConfigProperty(name = "one-stop.auth.xsrf.header")
    String xsrfHeaderName;

    @ConfigProperty(name = "one-stop.auth.access-token.cookie")
    String accessCookieName;
    @ConfigProperty(name = "one-stop.auth.access-token.expires-in")
    Duration accessCookieTimeout;

    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    String refreshCookieName;
    @ConfigProperty(name = "one-stop.auth.refresh-token.expires-in")
    Duration refreshCookieTimeout;

    @InjectMock
    @MockitoConfig(convertScopes = true)
    UserRepository userRepository;
    @InjectMock
    PasswordCrypto passwordCrypto;
    @InjectMock
    UserEventSender userEventSender;
    @InjectMock
    OpenIdAuthentication openIdAuth;

    @Test
    public void testGetJwkSet() {
        given()
            .get("/api/v1/auth/jwks.json")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .assertThat()
            .body("keys[0].kty", is("EC"));
    }

    @ParameterizedTest
    @EnumSource(AuthProvider.class)
    public void TestOauthLogin(AuthProvider authProvider) {
        // given: the auth-provider's redirect URI
        URI redirectUri = URI.create("http://mock-uri");
        when(openIdAuth.oauthLogin(eq(authProvider), any())).thenReturn(redirectUri);

        // when: the open-id login is initiated
        String state = randomAlphanumeric(20);
        URI response = given()
            .contentType(JSON)
            .queryParam("state", state)
            .get("/api/v1/auth/login/{auth-provider}", Map.of("auth-provider", authProvider.getProviderName()))
            .then()
            .statusCode(200)
            .extract().response().as(URI.class);

        // then: the response is the open-id redirect URI
        assertEquals(redirectUri, response);

        // and: the correct AuthProvider and state were passed
        verify(openIdAuth).oauthLogin(authProvider, state);
    }

    @Test
    public void testLogin_HappyPath() {
        // given: a user wishing to login
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // when: a user logs in
        Response loginResponse = passwordLogin(user);

        // then: the login event is issued
        verify(userEventSender).sendUserAuthenticated(user, null);

        // and: the auth-cookies are valid
        validateCookies(loginResponse);
    }

    @Test
    public void testLogin_InvalidPassword() {
        // given: a user wishing to login
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // when: a user logs in
        // given: a login request
        LoginRequest request = new LoginRequest()
            .username(user.getUsername())
            .password(randomAlphanumeric(30));

        // and: the password is valid
        when(passwordCrypto.verify(request.getPassword().toCharArray(), user.getPasswordHash()))
            .thenReturn(false);

        Response response = given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/auth/login")
            .then()
            .statusCode(401)
            .extract().response();

        // then: NO login event is issued
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());

        // and: a login failed event is issued
        verify(userEventSender).sendAuthenticationFailed(user.getUsername(), null, "Invalid password.");

        // and: the auth-cookies are NOT returned
        assertNull(response.cookie(xsrfCookieName));
        assertNull(response.cookie(accessCookieName));
        assertNull(response.cookie(refreshCookieName));
    }

    @Test
    public void testOauthLogin_HappyPath() {
        // given: a NEW user wishing to login
        User user = User.builder()
            .id(null)
            .username(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).then(invocation -> {
            if (user.getId() == null)
                user.setId(UUID.randomUUID());
            return user;
        });

        // and: an OpenID auth-provider and auth-code
        AuthProvider authProvider = AuthProvider.GOOGLE;
        String authCode = randomAlphanumeric(30);

        // and: the auth-provider exchanges the auth-code for token and a user record
        when(openIdAuth.oauthExchange(authProvider, authCode)).thenReturn(user);

        // when: the auth-provider calls the callback
        Response loginResponse = given()
            .redirects().follow(false)
            .contentType(JSON)
            .queryParam("code", authCode)
            .queryParam("state", randomAlphanumeric(10))
            .queryParam("scope", "openid,profile,email")
            .get("/api/v1/auth/validate/" + authProvider.name())
            .then()
            .statusCode(307) // temporary redirect
            .extract().response();

        // then: the new user is saved
        verify(userRepository).save(user);

        // and: a user-created event is issued
        verify(userEventSender).sendUserCreated(user);

        // and: a user-login event is issued
        verify(userEventSender).sendUserAuthenticated(user, authProvider);

        // and: the auth-cookies are valid
        validateCookies(loginResponse);

        // and: the redirect path is returned
        assertEquals("http://localhost:8081/", loginResponse.getHeader("Location"));
    }

    @Test
    public void testOauthLogin_OidError() {
        // given: an OpenID auth-provider and auth-code
        AuthProvider authProvider = AuthProvider.GOOGLE;
        String authCode = randomAlphanumeric(30);

        // and: the auth-provider returns an error
        String error = "SOME ERROR";
        String errorUri = "http://google/auth-error-help";

        // when: the auth-provider calls the callback
        Response loginResponse = given()
            .redirects().follow(false)
            .contentType(JSON)
            .queryParam("code", authCode)
            .queryParam("state", randomAlphanumeric(10))
            .queryParam("scope", "openid,profile,email")
            .queryParam("error", error)
            .queryParam("error_uri", errorUri)
            .get("/api/v1/auth/validate/" + authProvider.name())
            .then()
            .statusCode(307) // temporary redirect
            .extract().response();

        // then: the auth-code is not exchanged for tokens
        verify(openIdAuth, never()).oauthExchange(any(), anyString());

        // and: NO user is saved
        verify(userRepository, never()).save(any());

        // and: NO user-created event is issued
        verify(userEventSender,never()).sendUserCreated(any());

        // and: NO user-login event is issued
        verify(userEventSender, never()).sendUserAuthenticated(any(), any());

        // and: the xsrf-cookie is removed
        assertNull(loginResponse.cookie(xsrfCookieName));

        // and: the access-cookie is invalidated
        Cookie accessCookie = loginResponse.detailedCookie(refreshCookieName);
        assertEquals("", accessCookie.getValue());
        assertEquals(0, accessCookie.getMaxAge());

        // and: the refresh-cookie is invalidated
        Cookie refreshCookie = loginResponse.detailedCookie(refreshCookieName);
        assertEquals("", refreshCookie.getValue());
        assertEquals(0, refreshCookie.getMaxAge());

        // and: the redirect path is return - with error code
        assertEquals("http://localhost:8081/?error=SOME+ERROR", loginResponse.getHeader("Location"));
    }

    @Test
    public void testRefresh_HappyPath() {
        // given: a user wishing to login
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: the user logs in
        Response loginResponse = passwordLogin(user);

        // and: the auth-cookies are valid
        validateCookies(loginResponse);

        // when: the user refreshes their tokens
        Cookie xsrfCookie = loginResponse.detailedCookie(xsrfCookieName);
        Response refreshResponse = given()
            .contentType(JSON)
            .header(xsrfHeaderName, xsrfCookie.getValue())
            .cookie(loginResponse.detailedCookie(refreshCookieName))
            .cookie(xsrfCookie)
            .get("/api/v1/auth/refresh")
            .then()
            .statusCode(204)
            .extract().response();

        // and: the auth-cookies are refreshed
        validateCookies(refreshResponse);
    }

    @Test
    public void testRefresh_NoRefreshToken() {
        // given: a user wishing to login
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: the user logs in
        Response loginResponse = passwordLogin(user);

        // and: the auth-cookies are valid
        validateCookies(loginResponse);

        // when: the user refreshes their tokens - with no refresh token
        Cookie xsrfCookie = loginResponse.detailedCookie(xsrfCookieName);
        given()
            .contentType(JSON)
            .header(xsrfHeaderName, xsrfCookie.getValue())
            .cookie(refreshCookieName)
            .cookie(xsrfCookie)
            .get("/api/v1/auth/refresh")
            .then()
            .assertThat()
            .statusCode(401);
    }

    @Test
    public void testRefresh_InvalidToken() throws InterruptedException {
        // given: a user wishing to login
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: the user logs in
        Response loginResponse = passwordLogin(user);

        // and: the auth-cookies are valid
        validateCookies(loginResponse);

        Cookie xsrfCookie = loginResponse.detailedCookie(xsrfCookieName);
        Cookie refreshCookie = loginResponse.detailedCookie(refreshCookieName);

        // and: the refresh token is invalid
        Cookie invalidCookie = new Cookie.Builder(refreshCookieName, randomAlphanumeric(30))
            .setExpiryDate(new Date())
            .setMaxAge(1)
            .setPath(refreshCookie.getPath())
            .setSecured(refreshCookie.isSecured())
            .setHttpOnly(refreshCookie.isHttpOnly())
            .setSameSite(refreshCookie.getSameSite())
            .setComment(refreshCookie.getComment())
            .setVersion(refreshCookie.getVersion())
            .build();

        // and: wait for it to expire
        synchronized (this) {
            wait(2000);
        }

        // when: the user refreshes their tokens
        given()
            .contentType(JSON)
            .header(xsrfHeaderName, xsrfCookie.getValue())
            .cookie(invalidCookie)
            .cookie(xsrfCookie)
            .get("/api/v1/auth/refresh")
            .then()
            .assertThat()
            .statusCode(401);
    }

    @Test
    public void testLogout() {
        // given: a user wishing to login
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(30))
            .roles(Set.of("user"))
            .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: the user logs in
        Response loginResponse = passwordLogin(user);

        // when: the user logs out - cookies in request aren't strictly needed for logout
        Cookie xsrfCookie = loginResponse.detailedCookie(xsrfCookieName);
        Response response = given()
            .contentType(JSON)
            .header(xsrfHeaderName, xsrfCookie.getValue())
            .cookie(loginResponse.detailedCookie(refreshCookieName))
            .cookie(xsrfCookie)
            .get("/api/v1/auth/logout")
            .then()
            .statusCode(204)
            .extract().response();

        // then: the xsrf-cookie is removed
        assertNull(response.cookie(xsrfCookieName));

        // and: the access-cookie is invalidated
        Cookie accessCookie = response.detailedCookie(refreshCookieName);
        assertEquals("", accessCookie.getValue());
        assertEquals(0, accessCookie.getMaxAge());

        // and: the refresh-cookie is invalidated
        Cookie refreshCookie = response.detailedCookie(refreshCookieName);
        assertEquals("", refreshCookie.getValue());
        assertEquals(0, refreshCookie.getMaxAge());
    }

    private Response passwordLogin(User user) {
        // given: a login request
        LoginRequest request = new LoginRequest()
            .username(user.getUsername())
            .password(randomAlphanumeric(30));

        // and: the password is valid
        when(passwordCrypto.verify(request.getPassword().toCharArray(), user.getPasswordHash()))
            .thenReturn(true);

        return given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/auth/login")
            .then()
            .statusCode(204)
            .extract().response();
    }

    private void validateCookies(Response response) {
        // and: a xsrf-cookie is returned
        assertNotNull(response.detailedCookie(xsrfCookieName));

        // and: an access-cookie is returned
        Cookie accessCookie = response.detailedCookie(accessCookieName);
        assertNotNull(accessCookie);
        assertNull(accessCookie.getDomain());
        assertEquals("Strict", accessCookie.getSameSite());
        assertEquals("/api", accessCookie.getPath());
        assertEquals(accessCookieTimeout.toSeconds(), accessCookie.getMaxAge());
        assertTrue(new Date().before(accessCookie.getExpiryDate()));
        assertTrue(accessCookie.isHttpOnly());
        assertFalse(accessCookie.isSecured()); // TODO: change this when we use SSL

        // and: a refresh-cookie is returned
        Cookie refreshCookie = response.detailedCookie(refreshCookieName);
        assertNotNull(refreshCookie);
        assertNull(refreshCookie.getDomain());
        assertEquals("Strict", refreshCookie.getSameSite());
        assertEquals("/api/v1/auth/refresh", refreshCookie.getPath());
        assertEquals(refreshCookieTimeout.toSeconds(), refreshCookie.getMaxAge());
        assertTrue(new Date().before(refreshCookie.getExpiryDate()));
        assertTrue(refreshCookie.isHttpOnly());
        assertFalse(refreshCookie.isSecured()); // TODO: change this when we use SSL
    }
}
