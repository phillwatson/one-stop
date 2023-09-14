package com.hillayes.integration.test.user;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.UserProfileApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.onestop.api.*;
import com.hillayes.sim.email.SendWithBlueSimulator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class UserProfileTestIT extends ApiTestBase {
    @Test
    public void testGetAuthProviders() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        UserProfileApi userProfileApi = new UserProfileApi(user.getAuthTokens());

        // when: the user retrieves their open-id auth providers
        UserAuthProvidersResponse authProviders = userProfileApi.getAuthProviders();

        // then: the result is empty
        assertNotNull(authProviders);
        assertTrue(authProviders.getAuthProviders().isEmpty());
    }

    @Test
    public void testProfile() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        UserProfileApi userProfileApi = new UserProfileApi(user.getAuthTokens());

        // when: the user retrieves their profile
        UserProfileResponse profile = userProfileApi.getProfile();

        // then: the profile is returned
        assertNotNull(profile);

        // and: the profile fits
        assertEquals(user.getUsername(), profile.getUsername());
        assertEquals(user.getEmail().toLowerCase(), profile.getEmail().toLowerCase());
        assertEquals(user.getGivenName(), profile.getGivenName());

        // and: the response shows the user's role
        assertEquals(1, profile.getRoles().size());
        assertTrue(profile.getRoles().contains("user"));

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            // when: the user updates their profile
            UserProfileRequest updateProfileRequest = new UserProfileRequest()
                .username(randomAlphanumeric(20))
                .title(randomAlphanumeric(5))
                .givenName(randomAlphanumeric(10))
                .familyName(randomAlphanumeric(20))
                .preferredName(randomAlphanumeric(20))
                .locale(Locale.CHINESE.toLanguageTag())
                .email(randomAlphanumeric(20))
                .phone(randomNumeric(10));
            UserProfileResponse updateProfileResponse = userProfileApi.updateProfile(updateProfileRequest);

            // then: the response confirms update
            assertEquals(updateProfileRequest.getUsername(), updateProfileResponse.getUsername());
            assertEquals(updateProfileRequest.getTitle(), updateProfileResponse.getTitle());
            assertEquals(updateProfileRequest.getGivenName(), updateProfileResponse.getGivenName());
            assertEquals(updateProfileRequest.getFamilyName(), updateProfileResponse.getFamilyName());
            assertEquals(updateProfileRequest.getPreferredName(), updateProfileResponse.getPreferredName());
            assertEquals(updateProfileRequest.getEmail().toLowerCase(), updateProfileResponse.getEmail().toLowerCase());
            assertEquals(updateProfileRequest.getPhone(), updateProfileResponse.getPhone());
            assertEquals(updateProfileRequest.getLocale(), updateProfileResponse.getLocale());

            // and: an email is sent to user's old email address to confirm update
            List<LoggedRequest> toOldEmail = emailSim.verifyEmailSent(
                user.getEmail(), "Your account has been updated",
                await().atMost(Duration.ofSeconds(60)));
            assertEquals(1, toOldEmail.size());

            // and: an email is sent to new email address
            List<LoggedRequest> toNewEmail = emailSim.verifyEmailSent(
                updateProfileRequest.getEmail(), "Your account has been updated",
                await().atMost(Duration.ofSeconds(60)));
            assertEquals(1, toNewEmail.size());

            // when: the user retrieves profile again
            profile = userProfileApi.getProfile();

            // then: the profile reflects update
            assertEquals(updateProfileRequest.getUsername(), profile.getUsername());
            assertEquals(updateProfileRequest.getTitle(), profile.getTitle());
            assertEquals(updateProfileRequest.getGivenName(), profile.getGivenName());
            assertEquals(updateProfileRequest.getFamilyName(), profile.getFamilyName());
            assertEquals(updateProfileRequest.getPreferredName(), profile.getPreferredName());
            assertEquals(updateProfileRequest.getEmail().toLowerCase(), profile.getEmail().toLowerCase());
            assertEquals(updateProfileRequest.getPhone(), profile.getPhone());
            assertEquals(updateProfileRequest.getLocale(), profile.getLocale());
        }
    }

    @Test
    public void testChangePassword() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        UserProfileApi userProfileApi = new UserProfileApi(user.getAuthTokens());

        // when: the user changes their password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .oldPassword(user.getPassword())
            .newPassword(randomAlphanumeric(20));
        userProfileApi.changePassword(request);
    }

    @Test
    public void testChangePassword_MissingOldPassword() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        UserProfileApi userProfileApi = new UserProfileApi(user.getAuthTokens());

        // when: the user changes their password - without giving old password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .newPassword(randomAlphanumeric(20));

        // then: the service returns bad-request status
        withServiceError(userProfileApi.changePassword(request, 400), error -> {
            // and: the response is a service error identifying the missing parameter
            assertEquals("PARAMETER_MISSING", error.getMessageId());
            assertNotNull(error.getContextAttributes().stream()
                .filter(attr -> attr.getName().equals("parameter-name"))
                .filter(attr -> attr.getValue().equals("oldPassword"))
                .findFirst().orElse(null));
        });
    }

    @Test
    public void testChangePassword_MissingNewPassword() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        UserProfileApi userProfileApi = new UserProfileApi(user.getAuthTokens());

        // when: the user changes their password - without giving new password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .oldPassword(user.getPassword());

        // then: the service returns bad-request status
        withServiceError(userProfileApi.changePassword(request, 400), error -> {
            // and: the response is a service error identifying the missing parameter
            assertEquals("PARAMETER_MISSING", error.getMessageId());
            assertNotNull(error.getContextAttributes().stream()
                .filter(attr -> attr.getName().equals("parameter-name"))
                .filter(attr -> attr.getValue().equals("newPassword"))
                .findFirst().orElse(null));
        });
    }

    @Test
    public void testChangePassword_InvalidOldPassword() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        UserProfileApi userProfileApi = new UserProfileApi(user.getAuthTokens());

        // when: the user changes their password - without giving new password
        PasswordUpdateRequest request = new PasswordUpdateRequest()
            .oldPassword(randomAlphanumeric(10))
            .newPassword(randomAlphanumeric(10));

        // then: the service returns unauthorized status
        userProfileApi.changePassword(request, 401);
    }
}
