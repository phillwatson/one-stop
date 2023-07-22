package com.hillayes.user.resource;

import com.hillayes.auth.audit.RequestHeaders;
import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import com.hillayes.user.service.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.cdi.NullJsonWebToken;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import org.codehaus.plexus.util.cli.Arg;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserOnboardResourceTest extends TestBase {
    @ConfigProperty(name = "one-stop.auth.xsrf.cookie-name")
    String xsrfCookieName;
    @ConfigProperty(name = "one-stop.auth.access-token.cookie")
    String accessCookieName;
    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    String refreshCookieName;
    @ConfigProperty(name = "one-stop.auth.onboarding.token-expires-in")
    Duration tokenDuration;

    @InjectMock
    private UserRepository userRepository;

    @InjectMock
    private UserEventSender userEventSender;

    @InjectMock
    private PasswordCrypto passwordCrypto;

    @Inject
    private UserService userService;

    @BeforeEach
    public void beforeEach() {
        // simulate setting of ID on save
        when(userRepository.save(any())).then(invocation -> {
            User user = invocation.getArgument(0);
            return (user.getId() == null)
                ? user.toBuilder().id(UUID.randomUUID()).build()
                : user;
        });
    }

    @Test
    public void testRegisterAndOnboarding() {
        // given: a register-user request
        UserRegisterRequest registerRequest = new UserRegisterRequest()
            .email(randomAlphanumeric(30));

        // and: no other user has that email address
        when(userRepository.findByEmail(registerRequest.getEmail().toLowerCase()))
            .thenReturn(Optional.empty());

        // when: the endpoint is called
        given()
            .contentType(JSON)
            .body(registerRequest)
            .when()
            .post("/api/v1/users/onboard/register")
            .then()
            .statusCode(202); // accepted

        // then: a registration event is issued
        ArgumentCaptor<URI> tokenCaptor = ArgumentCaptor.forClass(URI.class);
        verify(userEventSender).sendUserRegistered(eq(registerRequest.getEmail()), eq(tokenDuration), tokenCaptor.capture());

        // and: the URI contains the auth-token
        String token = Arrays.stream(tokenCaptor.getValue().getQuery().split("&"))
            .filter(q -> q.toLowerCase().startsWith("token"))
            .findFirst()
            .map(q -> q.split("=")[1])
            .orElse(null);
        assertNotNull(token);

        // given: the user acknowledged email - with auth token
        UserCompleteRequest request = new UserCompleteRequest()
            .username(randomAlphanumeric(12))
            .password(randomAlphanumeric(15))
            .givenName(randomAlphanumeric(20))
            .token(token);

        // and: a password encryption will be performed
        String passwordHash = randomAlphanumeric(20);
        when(passwordCrypto.getHash(request.getPassword().toCharArray()))
            .thenReturn(passwordHash);

        // when: the endpoint is called
        Response response = given()
            .contentType(JSON)
            .header("Accept-Language", Locale.ENGLISH.toLanguageTag())
            .body(request)
            .when()
            .post("/api/v1/users/onboard/complete")
            .then()
            .statusCode(201)
            .extract().response();

        // then: a user-created event is issued
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userEventSender).sendUserCreated(userCaptor.capture());
        User user = userCaptor.getValue();

        // then: the new user is saved
        verify(userRepository).save(user);

        // and: the email address is taken from original registration
        assertEquals(registerRequest.getEmail().toLowerCase(), user.getEmail());

        // and: user properties are copied from request
        assertEquals(request.getUsername(), user.getUsername());
        assertEquals(request.getGivenName(), user.getGivenName());
        assertEquals(passwordHash, user.getPasswordHash());

        // and: the user roles are set
        assertEquals(Set.of("user"), user.getRoles());

        // and: the location header identifies user-profile endpoint
        String location = "http://localhost:8081/api/v1/profiles/" + user.getId().toString();
        assertEquals(location, response.header("Location"));

        // and: auth cookies are returned
        assertNotNull(response.detailedCookie(xsrfCookieName));
        assertNotNull(response.detailedCookie(accessCookieName));
        assertNotNull(response.detailedCookie(refreshCookieName));
    }

    @Test
    public void testOnboardUser_InvalidToken() {
        // given: a request to complete onboarding - with an invalid auth-token
        UserCompleteRequest request = new UserCompleteRequest()
            .username(randomAlphanumeric(12))
            .password(randomAlphanumeric(15))
            .givenName(randomAlphanumeric(20))
            .token(randomAlphanumeric(30));

        // when: the endpoint is called
        Response response = given()
            .contentType(JSON)
            .header("Accept-Language", Locale.ENGLISH.toLanguageTag())
            .body(request)
            .when()
            .post("/api/v1/users/onboard/complete")
            .then()
            .statusCode(401)
            .extract().response();

        // then: NO user-created event is issued
        verify(userEventSender, never()).sendUserCreated(any());

        // then: NO user is saved
        verify(userRepository, never()).save(any());

        // and: NO location header identifies user-profile endpoint
        assertNull(response.header("Location"));

        // and: NO auth cookies are returned
        assertNull(response.detailedCookie(xsrfCookieName));
        assertNull(response.detailedCookie(accessCookieName));
        assertNull(response.detailedCookie(refreshCookieName));
    }
}
