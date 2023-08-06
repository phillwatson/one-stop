package com.hillayes.user.resource;

import com.hillayes.onestop.api.*;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserProfileResourceTest extends TestBase {
    @InjectMock
    private UserService userService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetProfile() {
        // given: a user exists
        UUID userId = UUID.fromString(userIdStr);
        User user = User.builder()
            .id(userId)
            .username(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .email(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(10))
            .passwordHash(UUID.randomUUID().toString())
            .locale(Locale.ENGLISH)
            .dateCreated(Instant.now().minusSeconds(20000))
            .dateOnboarded(Instant.now().minusSeconds(10000))
            .roles(Set.of("user"))
            .build();
        when(userService.getUser(userId)).thenReturn(Optional.of(user));

        // when: the user asks for their profile
        UserProfileResponse response = given()
            .when()
            .get("/api/v1/profiles")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(UserProfileResponse.class);

        // and: the response is correct
        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getTitle(), response.getTitle());
        assertEquals(user.getGivenName(), response.getGivenName());
        assertEquals(user.getFamilyName(), response.getFamilyName());
        assertEquals(user.getPreferredName(), response.getPreferredName());
        assertEquals(user.getPhoneNumber(), response.getPhone());
        assertEquals(user.getDateCreated(), response.getDateCreated());
        assertEquals(user.getDateOnboarded(), response.getDateOnboarded());
        assertEquals(user.getLocale().toLanguageTag(), response.getLocale());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetProfile_NotFound() {
        // given: a user exists
        UUID userId = UUID.fromString(userIdStr);
        when(userService.getUser(userId)).thenReturn(Optional.empty());

        // when: the user asks for their profile
        ServiceError response = given()
            .when()
            .get("/api/v1/profiles")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract().as(ServiceError.class);

        // then: the get-user service is called
        verify(userService).getUser(userId);

        // and: the response details the error
        verifyError(response, userId);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateProfile() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);

        // and: the user can be found
        when(userService.updateUser(eq(userId), any())).then(invocation -> Optional.of(invocation.getArgument(1)));

        // and: the user requests an update to their profile
        UserProfileRequest request = new UserProfileRequest()
            .email(randomAlphanumeric(30))
            .username(randomAlphanumeric(30))
            .title(randomAlphanumeric(30))
            .givenName(randomAlphanumeric(30))
            .familyName(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(30))
            .locale(Locale.CHINESE.toLanguageTag())
            .phone(randomNumeric(12));

        // when: the user asks for their profile
        UserProfileResponse response = given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/profiles")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserProfileResponse.class);

        // and: the response is correct
        assertNotNull(response);
        assertEquals(request.getUsername(), response.getUsername());
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(request.getTitle(), response.getTitle());
        assertEquals(request.getGivenName(), response.getGivenName());
        assertEquals(request.getFamilyName(), response.getFamilyName());
        assertEquals(request.getPreferredName(), response.getPreferredName());
        assertEquals(request.getPhone(), response.getPhone());
        assertEquals(request.getLocale(), response.getLocale());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateProfile_NotFound() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);

        // and: the user cannot be found
        when(userService.updateUser(eq(userId), any())).thenReturn(Optional.empty());

        // and: the user requests an update to their profile
        UserProfileRequest request = new UserProfileRequest()
            .email(randomAlphanumeric(30))
            .username(randomAlphanumeric(30))
            .title(randomAlphanumeric(30))
            .givenName(randomAlphanumeric(30))
            .familyName(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(30))
            .locale(Locale.CHINESE.toLanguageTag())
            .phone(randomNumeric(12));

        // when: the user asks for their profile
        ServiceError response = given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/profiles")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract().as(ServiceError.class);

        // and: the update-user service is called
        verify(userService).updateUser(eq(userId), any());

        // and: the response details the error
        verifyError(response, userId);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetUserAuthProviders() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);
        User user = User.builder()
            .id(userId)
            .username(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .email(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(10))
            .passwordHash(UUID.randomUUID().toString())
            .locale(Locale.ENGLISH)
            .dateCreated(Instant.now().minusSeconds(20000))
            .dateOnboarded(Instant.now().minusSeconds(10000))
            .roles(Set.of("user"))
            .build();
        when(userService.getUser(userId)).thenReturn(Optional.of(user));

        // and: the user has a collection of Open-ID identifiers
        List<OidcIdentity> oidcIdentities = Arrays.stream(AuthProvider.values()).map(authProvider ->
            OidcIdentity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(authProvider)
                .issuer(randomAlphanumeric(20))
                .subject(randomAlphanumeric(15))
                .dateCreated(Instant.now())
                .build()
        ).toList();
        when(userService.getUserAuthProviders(userId)).thenReturn(oidcIdentities);

        // when: the user asks for their oidc identifiers
        UserAuthProvidersResponse response = given()
            .contentType(JSON)
            .when()
            .get("/api/v1/profiles/authproviders")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserAuthProvidersResponse.class);

        // then: the response contains all user's OIDC identifiers
        oidcIdentities.forEach(expected ->
            assertNotNull(response.getAuthProviders().stream()
                .filter(identifier -> identifier.getName().equals(expected.getProvider().getProviderName()))
                .findFirst().orElse(null))
        );
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testChangePassword() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);
        User user = User.builder()
            .id(userId)
            .username(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .email(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(10))
            .passwordHash(UUID.randomUUID().toString())
            .locale(Locale.ENGLISH)
            .dateCreated(Instant.now().minusSeconds(20000))
            .dateOnboarded(Instant.now().minusSeconds(10000))
            .roles(Set.of("user"))
            .build();

        // and: a request to change password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .oldPassword(randomAlphanumeric(20))
            .newPassword(randomAlphanumeric(20));

        // and: the user can be found
        when(userService.updatePassword(userId,
            request.getOldPassword().toCharArray(),
            request.getNewPassword().toCharArray()))
            .then(invocation -> Optional.of(user));

        given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/profiles/password")
            .then()
            .statusCode(204); // no content

        // and: the service is called to update password
        verify(userService).updatePassword(userId,
            request.getOldPassword().toCharArray(),
            request.getNewPassword().toCharArray());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testChangePassword_NotFound() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);

        // and: a request to change password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .oldPassword(randomAlphanumeric(20))
            .newPassword(randomAlphanumeric(20));

        // and: the user can be found
        when(userService.updatePassword(userId,
            request.getOldPassword().toCharArray(),
            request.getNewPassword().toCharArray()))
            .then(invocation -> Optional.empty());

        given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/profiles/password")
            .then()
            .statusCode(401); // not authorised - to disguise condition

        // and: the service is called to update password
        verify(userService).updatePassword(userId,
            request.getOldPassword().toCharArray(),
            request.getNewPassword().toCharArray());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testChangePassword_MissingOldPassword() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);

        // and: a request to change password - no old password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .newPassword(randomAlphanumeric(20));

        given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/profiles/password")
            .then()
            .statusCode(400);

        // and: no update is performed
        verify(userService, never()).updatePassword(any(), any(), any());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testChangePassword_MissingNewPassword() {
        // given: an identified user
        UUID userId = UUID.fromString(userIdStr);

        // and: a request to change password - no old password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .oldPassword(randomAlphanumeric(20));

        given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/profiles/password")
            .then()
            .statusCode(400);

        // and: no update is performed
        verify(userService, never()).updatePassword(any(), any(), any());
    }

    private void verifyError(ServiceError response, UUID userId) {
        assertNotNull(response);
        assertNotNull(response.getCorrelationId());
        assertEquals(ServiceError.SeverityEnum.INFO, response.getSeverity());
        assertEquals("ENTITY_NOT_FOUND", response.getMessageId());
        assertEquals("The identified entity cannot be found.", response.getMessage());

        assertNotNull(response.getContextAttributes());
        assertEquals("user", response.getContextAttributes().stream()
            .filter(attr -> attr.getName().equals("entity-type"))
            .findFirst()
            .map(ContextAttribute::getValue)
            .orElse(null));
        assertEquals(userId.toString(), response.getContextAttributes().stream()
            .filter(attr -> attr.getName().equals("entity-id"))
            .findFirst()
            .map(ContextAttribute::getValue)
            .orElse(null));
    }
}
