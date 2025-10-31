package com.hillayes.integration.test.util;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.user.UserOnboardApi;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import com.hillayes.sim.email.EmailMessage;
import com.hillayes.sim.email.SendInBlueSimulator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class UserUtils {
    public static List<UserEntity> createUsers(int wiremockPort,
                                               List<UserEntity> users) {
        try (SendInBlueSimulator emailSim = new SendInBlueSimulator(wiremockPort)) {
            return users.stream()
                .map(user -> __createUser(emailSim, user))
                .toList();
        }
    }

    public static UserEntity createUser(int wiremockPort,
                                         UserEntity user) {
        try (SendInBlueSimulator emailSim = new SendInBlueSimulator(wiremockPort)) {
            return __createUser(emailSim, user);
        }
    }

    private static UserEntity __createUser(SendInBlueSimulator emailSim,
                                           UserEntity user) {
        log.info("Creating user [username: {}]", user.getUsername());
        UserOnboardApi userOnboardApi = new UserOnboardApi();

        UserRegisterRequest registerRequest = new UserRegisterRequest()
            .email(user.getEmail());
        userOnboardApi.registerUser(registerRequest);

        // wait for registration email containing magic-token
        List<LoggedRequest> emailRequests = emailSim.verifyEmailSent(user.getEmail(),
            await().atMost(Duration.ofSeconds(60)));

        EmailMessage emailMessage = emailSim.parse(emailRequests.get(0));

        // extract the magic-link token
        String htmlContent = emailMessage.getHtmlContent();
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
        Pair<UUID, Map<String, String>> auth = userOnboardApi.onboardUser(completeRequest);

        Map<String, String> authTokens = auth.getRight();
        assertEquals(3, authTokens.size());
        assertTrue(authTokens.containsKey("access_token"));
        assertTrue(authTokens.containsKey("refresh_token"));
        assertTrue(authTokens.containsKey("XSRF-TOKEN"));

        user.setId(auth.getLeft());
        user.setAuthTokens(authTokens);

        log.info("Created user [username: {}]", user.getUsername());

        // wait for welcome email
        emailSim.verifyEmailSent(user.getEmail(), "Welcome to One-Stop",
            await().atMost(Duration.ofSeconds(60)));

        return user;
    }
}
