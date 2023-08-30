package com.hillayes.integration.test.user;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.UserOnboardApi;
import com.hillayes.integration.api.UserProfileApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.sim.email.EmailMessage;
import com.hillayes.integration.test.sim.email.SendWithBlueSimulator;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserProfileResponse;
import com.hillayes.onestop.api.UserRegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class UserOnboardTest extends ApiTestBase {
    private SendWithBlueSimulator emailSim;

    @BeforeEach
    public void startEmailSim() {
        emailSim = new SendWithBlueSimulator(getWiremockPort());
    }

    @AfterEach
    public void stopEmailSim() {
        emailSim.close();
    }

    @Test
    public void testRegisterAndOnboardNewUser() {
        UserOnboardApi userOnboardApi = new UserOnboardApi();

        // given: a request to register a user
        UserRegisterRequest registerRequest = new UserRegisterRequest()
            .email(randomAlphanumeric(30));

        // when: the request is sent to the service
        userOnboardApi.registerUser(registerRequest);

        // then: an email is sent to the user
        List<LoggedRequest> emailRequests = emailSim.verifyEmailSent(registerRequest.getEmail(),
            await().atMost(Duration.ofSeconds(30)));
        assertNotNull(emailRequests);
        assertEquals(1, emailRequests.size());

        // and: the email contains HTMl body content
        EmailMessage emailMessage = emailSim.parse(emailRequests.get(0));
        String htmlContent = emailMessage.getHtmlContent();
        assertNotNull(htmlContent);

        // and: the HTML contains the magic-link token
        int startIndex = htmlContent.indexOf("onboard-user?token=") + "onboard-user?token=".length();
        int endIndex = htmlContent.indexOf("\">", startIndex);
        String token = htmlContent.substring(startIndex, endIndex);
        assertNotNull(token);

        // when: the magic-link token is returned to the server to complete registration
        UserCompleteRequest completeRequest = new UserCompleteRequest()
            .token(token)
            .username(randomAlphanumeric(12))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(15));
        Map<String, String> authTokens = userOnboardApi.onboardUser(completeRequest).getRight();

        // then: auth tokens are returned
        assertEquals(3, authTokens.size());
        assertTrue(authTokens.containsKey("access_token"));
        assertTrue(authTokens.containsKey("refresh_token"));
        assertTrue(authTokens.containsKey("XSRF-TOKEN"));

        // and: the new user can ue these tokens to retrieve their profile
        UserProfileResponse profile = new UserProfileApi(authTokens).getProfile();
        assertNotNull(profile);

        // and: the profile matches given user information
        assertEquals(completeRequest.getUsername(), profile.getUsername());
        assertEquals(completeRequest.getGivenName(), profile.getGivenName());
        assertEquals(registerRequest.getEmail().toLowerCase(), profile.getEmail().toLowerCase());
    }
}
