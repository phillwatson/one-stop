package com.hillayes.integration.test.user;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.AuthApi;
import com.hillayes.integration.api.UserAdminApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.onestop.api.PaginatedUsers;
import com.hillayes.onestop.api.UserResponse;
import com.hillayes.onestop.api.UserRole;
import com.hillayes.onestop.api.UserUpdateRequest;
import com.hillayes.sim.email.SendWithBlueSimulator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserAdminTestIT extends ApiTestBase {
    @Test
    public void testListUsers() {
        // given: several users exist
        List<UserEntity> expectedUsers = mockUsers();

        // and: the admin user signs in
        AuthApi authApi = new AuthApi();
        Map<String, String> authTokens = authApi.login("admin", "password");
        assertNotNull(authTokens);
        assertEquals(3, authTokens.size());

        // when: the admin lists the users
        UserAdminApi userAdminApi = new UserAdminApi(authTokens);
        PaginatedUsers users = userAdminApi.listUsers(0, 30);
        assertNotNull(users);

        // then: the results reflects selected page index and size
        assertEquals(0, users.getPage());
        assertEquals(30, users.getPageSize());

        // and: the admin user is in the list
        assertNotNull(users.getItems());
        assertNotNull(users.getItems().stream()
            .filter(user -> "admin".equals(user.getUsername()))
            .findFirst().orElse(null));

        // and: all other user are in list
        expectedUsers.forEach(expected ->
            assertNotNull(users.getItems().stream()
                .filter(user -> expected.getId().equals(user.getId()))
                .filter(user -> expected.getUsername().equals(user.getUsername()))
                .findFirst().orElse(null))
        );
    }

    @Test
    public void testGetUser() {
        // given: several users exist
        List<UserEntity> expectedUsers = mockUsers();

        // and: the admin user signs in
        AuthApi authApi = new AuthApi();
        Map<String, String> authTokens = authApi.login("admin", "password");
        assertNotNull(authTokens);
        assertEquals(3, authTokens.size());

        // when: the admin retrieves each user
        UserAdminApi userAdminApi = new UserAdminApi(authTokens);
        expectedUsers.forEach(expected -> {
            UserResponse user = userAdminApi.getUser(expected.getId());

            // then: the data matches that given in the registration
            assertEquals(expected.getId(), user.getId());
            assertEquals(expected.getUsername(), user.getUsername());
            assertEquals(expected.getGivenName(), user.getGivenName());
            assertEquals(expected.getEmail().toLowerCase(), user.getEmail().toLowerCase());

            // and: the user has the role "user"
            assertEquals(1, user.getRoles().size());
            assertEquals("user", user.getRoles().get(0));
        });
    }

    @Test
    public void testUpdateUser() {
        // given: several users exist
        List<UserEntity> expectedUsers = mockUsers();

        // and: the admin user signs in
        AuthApi authApi = new AuthApi();
        Map<String, String> authTokens = authApi.login("admin", "password");
        assertNotNull(authTokens);
        assertEquals(3, authTokens.size());

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            // when: the admin user updates each user
            UserAdminApi adminApi = new UserAdminApi(authTokens);
            expectedUsers.forEach(user -> {
                UserResponse userDetails = adminApi.getUser(user.getId());
                assertEquals(user.getId(), userDetails.getId());

                UserUpdateRequest updateRequest = new UserUpdateRequest()
                    .username(randomStrings.nextAlphanumeric(20))
                    .email(randomStrings.nextAlphabetic(20))
                    .preferredName(randomStrings.nextAlphanumeric(20))
                    .title(randomStrings.nextAlphabetic(5))
                    .givenName(randomStrings.nextAlphanumeric(20))
                    .familyName(randomStrings.nextAlphanumeric(20))
                    .phone(randomStrings.nextNumeric(8))
                    .locale(Locale.FRANCE.toLanguageTag())
                    .roles(userDetails.getRoles().stream().map(UserRole::fromValue).toList());
                UserResponse updatedUser = adminApi.updateUser(user.getId(), updateRequest);

                // then: the data matches that given in the request
                assertEquals(updateRequest.getUsername(), updatedUser.getUsername());
                assertEquals(updateRequest.getEmail().toLowerCase(), updatedUser.getEmail().toLowerCase());
                assertEquals(updateRequest.getPreferredName(), updatedUser.getPreferredName());
                assertEquals(updateRequest.getTitle(), updatedUser.getTitle());
                assertEquals(updateRequest.getGivenName(), updatedUser.getGivenName());
                assertEquals(updateRequest.getFamilyName(), updatedUser.getFamilyName());
                assertEquals(updateRequest.getPhone(), updatedUser.getPhone());
                assertEquals(updateRequest.getLocale(), updatedUser.getLocale());
                assertEquals(updateRequest.getRoles().stream().map(UserRole::getValue).toList(), updatedUser.getRoles());

                // and: an email is sent to old email address
                List<LoggedRequest> toOldEmail = emailSim.verifyEmailSent(
                    user.getEmail(), "Your account has been updated",
                    await().atMost(Duration.ofSeconds(60)));
                assertEquals(1, toOldEmail.size());

                // and: an email is sent to new email address
                List<LoggedRequest> toNewEmail = emailSim.verifyEmailSent(
                    updatedUser.getEmail(), "Your account has been updated",
                    await().atMost(Duration.ofSeconds(60)));
                assertEquals(1, toNewEmail.size());
            });
        }
    }

    @Test
    public void testDeleteUser() {
        // given: the admin user signs in
        AuthApi authApi = new AuthApi();
        Map<String, String> authTokens = authApi.login("admin", "password");
        assertNotNull(authTokens);
        assertEquals(3, authTokens.size());

        // and: the number of users is known
        UserAdminApi adminApi = new UserAdminApi(authTokens);
        long initialUserCount = adminApi.listUsers(0, 30).getTotal();

        // and: several users are created
        List<UserEntity> expectedUsers = mockUsers();

        // and: the user count reflects the new users
        assertEquals(initialUserCount + expectedUsers.size(),
            adminApi.listUsers(0, 30).getTotal());

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            expectedUsers.forEach(user -> {
                // when: admin deletes the user
                adminApi.deleteUser(user.getId());

                // then: an email is sent to the user
                List<LoggedRequest> emailRequests = emailSim.verifyEmailSent(
                    user.getEmail(), "Sorry to see you go",
                    await().atMost(Duration.ofSeconds(60)));
                assertEquals(1, emailRequests.size());
            });
        }

        // and: the user count is back to the original
        assertEquals(initialUserCount, adminApi.listUsers(0, 30).getTotal());
    }

    private List<UserEntity> mockUsers() {
        return UserUtils.createUsers(getWiremockPort(),
            List.of(
                UserEntity.builder()
                    .username(randomStrings.nextAlphanumeric(15))
                    .givenName(randomStrings.nextAlphanumeric(10))
                    .email(randomStrings.nextAlphanumeric(20))
                    .password(randomStrings.nextAlphanumeric(12)).build(),
                UserEntity.builder()
                    .username(randomStrings.nextAlphanumeric(15))
                    .givenName(randomStrings.nextAlphanumeric(10))
                    .email(randomStrings.nextAlphanumeric(20))
                    .password(randomStrings.nextAlphanumeric(12)).build(),
                UserEntity.builder()
                    .username(randomStrings.nextAlphanumeric(15))
                    .givenName(randomStrings.nextAlphanumeric(10))
                    .email(randomStrings.nextAlphanumeric(20))
                    .password(randomStrings.nextAlphanumeric(12)).build()
            )
        );
    }
}
