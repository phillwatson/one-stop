package com.hillayes.user.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import com.hillayes.user.utils.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.user.utils.TestData.mockUsers;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserAdminResourceTest extends TestBase {
    @InjectMock
    private UserService userService;

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testListUsers() {
        List<User> users = mockUsers(15);

        Page<User> pagedUsers = new Page<>(users, 310, 10, 20);
        when(userService.listUsers(anyInt(), anyInt())).thenReturn(pagedUsers);

        PaginatedUsers response = given()
            .when()
            .queryParam("page", pagedUsers.getPageIndex())
            .queryParam("page-size", pagedUsers.getPageSize())
            .get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedUsers.class);

        // then: the user-service is called with the page parameters
        verify(userService).listUsers(pagedUsers.getPageIndex(), pagedUsers.getPageSize());

        // and: the response corresponds to the paged list of accounts
        assertEquals(users.size(), response.getCount());
        assertNotNull(response.getItems());
        assertEquals(users.size(), response.getItems().size());
        assertEquals(pagedUsers.getTotalCount(), response.getTotal());
        assertEquals(pagedUsers.getPageIndex(), response.getPage());
        assertEquals(pagedUsers.getPageSize(), response.getPageSize());

        // and: all page links are present
        assertEquals("/api/v1/users", response.getLinks().getFirst().getPath());
        assertEquals("page-size=20&page=0", response.getLinks().getFirst().getQuery());

        assertNotNull(response.getLinks().getPrevious());
        assertEquals("/api/v1/users", response.getLinks().getPrevious().getPath());
        assertEquals("page-size=20&page=9", response.getLinks().getPrevious().getQuery());

        assertNotNull(response.getLinks().getNext());
        assertEquals("/api/v1/users", response.getLinks().getNext().getPath());
        assertEquals("page-size=20&page=11", response.getLinks().getNext().getQuery());

        assertEquals("/api/v1/users", response.getLinks().getLast().getPath());
        assertEquals("page-size=20&page=15", response.getLinks().getLast().getQuery());
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testListUsers_DefaultPageSize() {
        List<User> users = mockUsers(15);

        Page<User> pagedUsers = new Page<>(users, 310, 0, 20);
        when(userService.listUsers(anyInt(), anyInt())).thenReturn(pagedUsers);

        PaginatedUsers response = given()
            .when()
            .get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedUsers.class);

        // then: the user-service is called with the page parameters
        verify(userService).listUsers(pagedUsers.getPageIndex(), pagedUsers.getPageSize());

        // and: the response shows the default page size
        assertEquals(pagedUsers.getPageIndex(), response.getPage());
        assertEquals(pagedUsers.getPageSize(), response.getPageSize());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testListUsers_NotAuthorised() {
        List<User> users = mockUsers(15);

        Page<User> pagedUsers = new Page<>(users,310, 10, 15);
        when(userService.listUsers(anyInt(), anyInt())).thenReturn(pagedUsers);

        given()
            .when()
            .queryParam("page", pagedUsers.getPageIndex())
            .queryParam("page-size", pagedUsers.getPageSize())
            .get("/api/v1/users")
            .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testGetUser() {
        // given: a collection of users
        List<User> users = IntStream.range(0, 5)
            .mapToObj(i -> TestData.mockUser(UUID.randomUUID()))
            .toList();

        when(userService.getUser(any())).then(invocation -> {
            UUID userId = invocation.getArgument(0);
            return users.stream()
                .filter(u -> userId.equals(u.getId()))
                .findFirst();
        });

        // when: the admin requests each user
        users.forEach(user -> {
            UserResponse response = given()
                .when()
                .get("/api/v1/users/{userId}", user.getId())
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .as(UserResponse.class);

            assertNotNull(response);
            assertEquals(user.getId(), response.getId());
            assertEquals(user.getUsername(), response.getUsername());
            assertEquals(user.getPreferredName(), response.getPreferredName());
            assertEquals(user.getTitle(), response.getTitle());
            assertEquals(user.getGivenName(), response.getGivenName());
            assertEquals(user.getFamilyName(), response.getFamilyName());
            assertEquals(user.getEmail(), response.getEmail());
            assertEquals(user.getPhoneNumber(), response.getPhone());
            assertEquals(user.getLocale().toLanguageTag(), response.getLocale());
            assertEquals(user.getDateCreated(), response.getDateCreated());
            assertEquals(user.getDateOnboarded(), response.getDateOnboarded());
            assertEquals(user.getDateBlocked(), response.getDateBlocked());
            assertEquals(user.getRoles(), new HashSet<>(response.getRoles()));
        });
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testGetUser_NotFound() {
        // given: an unknown user ID
        UUID userId = UUID.randomUUID();

        // and: the user is not found
        when(userService.getUser(userId)).thenReturn(Optional.empty());

        // when: the admin requests the user
        ServiceErrorResponse response = given()
            .when()
            .get("/api/v1/users/{userId}", userId)
            .then()
            .contentType(JSON)
            .statusCode(404)
            .extract()
            .as(ServiceErrorResponse.class);

        // and: the response details the error
        verifyError(response, userId);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetUser_NotAuthorised() {
        // given: a user
        User user = TestData.mockUser(UUID.randomUUID());

        // when: a user makes a requests
        given()
            .when()
            .get("/api/v1/users/{userId}", user.getId())
            .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testUpdateUser() {
        // given: a user to be updated
        User user = TestData.mockUser(UUID.randomUUID());
        when(userService.updateUser(eq(user.getId()), any())).thenReturn(Optional.of(user));

        // and: a request to update user
        UserUpdateRequest request = new UserUpdateRequest()
            .username(randomAlphanumeric(20))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(15))
            .preferredName(randomAlphanumeric(10))
            .addRolesItem(UserRole.ADMIN)
            .email(randomAlphanumeric(30))
            .phone(randomNumeric(10));

        // when: the admin makes an update requests
        UserResponse response = given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/users/{userId}", user.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(UserResponse.class);

        assertNotNull(response);
        assertEquals(user.getId(), response.getId());
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getPreferredName(), response.getPreferredName());
        assertEquals(user.getTitle(), response.getTitle());
        assertEquals(user.getGivenName(), response.getGivenName());
        assertEquals(user.getFamilyName(), response.getFamilyName());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getPhoneNumber(), response.getPhone());
        assertEquals(user.getLocale().toLanguageTag(), response.getLocale());
        assertEquals(user.getDateCreated(), response.getDateCreated());
        assertEquals(user.getDateOnboarded(), response.getDateOnboarded());
        assertEquals(user.getDateBlocked(), response.getDateBlocked());
        assertEquals(user.getRoles(), new HashSet<>(response.getRoles()));
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testUpdateUser_NotFound() {
        // given: an unknown user ID
        UUID userId = UUID.randomUUID();
        when(userService.updateUser(eq(userId), any())).thenReturn(Optional.empty());

        // and: a request to update user
        UserUpdateRequest request = new UserUpdateRequest()
            .username(randomAlphanumeric(20))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(15))
            .preferredName(randomAlphanumeric(10))
            .addRolesItem(UserRole.ADMIN)
            .email(randomAlphanumeric(30))
            .phone(randomNumeric(10));

        // when: the admin makes an update requests
        ServiceErrorResponse response = given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/users/{userId}", userId)
            .then()
            .contentType(JSON)
            .statusCode(404)
            .extract()
            .as(ServiceErrorResponse.class);

        // and: the response details the error
        verifyError(response, userId);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateUser_NotAuthorised() {
        // given: a user
        User user = TestData.mockUser(UUID.randomUUID());

        // and: a request to update user
        UserUpdateRequest request = new UserUpdateRequest()
            .username(randomAlphanumeric(20))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(15))
            .preferredName(randomAlphanumeric(10))
            .addRolesItem(UserRole.ADMIN)
            .email(randomAlphanumeric(30))
            .phone(randomNumeric(10));

        // when: a user makes a requests
        given()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/users/{userId}", user.getId())
            .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testDeleteUser() {
        // given: a collection of users
        List<User> users = IntStream.range(0, 5)
            .mapToObj(i -> TestData.mockUser(UUID.randomUUID()))
            .toList();

        when(userService.deleteUser(any())).then(invocation -> {
            UUID userId = invocation.getArgument(0);
            return users.stream()
                .filter(u -> userId.equals(u.getId()))
                .findFirst();
        });

        // when: the admin requests each user
        users.forEach(user -> {
            given()
                .when()
                .delete("/api/v1/users/{userId}", user.getId())
                .then()
                .statusCode(204); // no content
        });
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testDeleteUser_NotFound() {
        // given: an unknown user ID
        UUID userId = UUID.randomUUID();

        when(userService.deleteUser(userId)).thenReturn(Optional.empty());

        // when: the admin requests each user
        ServiceErrorResponse response = given()
            .when()
            .delete("/api/v1/users/{userId}", userId)
            .then()
            .contentType(JSON)
            .statusCode(404)
            .extract()
            .as(ServiceErrorResponse.class);

        // and: the response details the error
        verifyError(response, userId);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteUser_NotAuthorised() {
        // given: a user
        User user = TestData.mockUser(UUID.randomUUID());

        // when: a user makes a requests
        given()
            .when()
            .delete("/api/v1/users/{userId}", user.getId())
            .then()
            .statusCode(403);
    }

    private void verifyError(ServiceErrorResponse response, UUID userId) {
        assertNotNull(response);
        assertNotNull(response.getCorrelationId());
        assertNotNull(response.getErrors());

        ServiceError serviceError = response.getErrors().get(0);
        assertEquals(ErrorSeverity.INFO, serviceError.getSeverity());
        assertEquals("ENTITY_NOT_FOUND", serviceError.getMessageId());
        assertEquals("The identified entity cannot be found.", serviceError.getMessage());

        assertNotNull(serviceError.getContextAttributes());
        assertEquals("User", serviceError.getContextAttributes().get("entity-type"));
        assertEquals(userId.toString(), serviceError.getContextAttributes().get("entity-id"));
    }
}
