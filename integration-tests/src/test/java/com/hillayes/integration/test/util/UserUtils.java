package com.hillayes.integration.test.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.UserOnboardApi;
import com.hillayes.integration.test.sim.email.SendWithBlueSimulator;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class UserUtils {
    public static List<UserEntity> createUsers(int wiremockPort,
                                               List<UserEntity> users) throws Exception {
        SendWithBlueSimulator emailSim = new SendWithBlueSimulator(wiremockPort);
        try {
            return users.stream()
                .map(user -> __createUser(emailSim, user))
                .toList();
        } finally {
            emailSim.stop();
        }
    }

    private static UserEntity createUser(int wiremockPort,
                                         UserEntity user) throws RuntimeException {
        SendWithBlueSimulator emailSim = new SendWithBlueSimulator(wiremockPort);
        try {
            return __createUser(emailSim, user);
        } finally {
            emailSim.stop();
        }
    }

    private static UserEntity __createUser(SendWithBlueSimulator emailSim,
                                           UserEntity user) throws RuntimeException {
        log.info("Creating user [username: {}]", user.getUsername());
        UserOnboardApi userOnboardApi = new UserOnboardApi();

        UserRegisterRequest registerRequest = new UserRegisterRequest()
            .email(user.getEmailAddress());
        userOnboardApi.registerUser(registerRequest);

        // wait for registration email containing magic-token
        List<LoggedRequest> emailRequests = emailSim.verifyEmailSent(user.getEmailAddress(),
            await().atMost(Duration.ofSeconds(60)));

        String htmlContent;
        try {
            JsonNode json = new ObjectMapper().readTree(emailRequests.get(0).getBodyAsString());
            htmlContent = json.path("htmlContent").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // extract the magic-link token
        int startIndex = htmlContent.indexOf("onboard-user?token=") + "onboard-user?token=".length();
        int endIndex = htmlContent.indexOf("\">", startIndex);
        String token = htmlContent.substring(startIndex, endIndex);

        // return the magic-link token to the server to complete registration
        UserCompleteRequest completeRequest = new UserCompleteRequest()
            .token(token)
            .username(user.getUsername())
            .givenName(user.getGivenName())
            .password(user.getPassword());

        // get the auth-tokens from response
        Map<String, String> authTokens = userOnboardApi.onboardUser(completeRequest);
        assertEquals(3, authTokens.size());
        assertTrue(authTokens.containsKey("access_token"));
        assertTrue(authTokens.containsKey("refresh_token"));
        assertTrue(authTokens.containsKey("XSRF-TOKEN"));

        user.setAuthTokens(authTokens);
        log.info("Created user [username: {}]", user.getUsername());
        return user;
    }
}
