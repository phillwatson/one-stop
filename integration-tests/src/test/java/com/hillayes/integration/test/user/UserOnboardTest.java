package com.hillayes.integration.test.user;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.UserOnboardApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.sim.email.SendWithBlueSimulator;
import com.hillayes.onestop.api.UserRegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserOnboardTest extends ApiTestBase {
    private SendWithBlueSimulator emailSim;

    @BeforeEach
    public void startEmailSim() {
        emailSim = new SendWithBlueSimulator(getWiremockPort());
    }

    @AfterEach
    public void stopEmailSim() {
        emailSim.stop();
    }

    @Test
    public void testRegister() {
        UserOnboardApi userOnboardApi = new UserOnboardApi();

        // given: a request to register a user
        UserRegisterRequest request = new UserRegisterRequest();
        request.email("watson.phill@gmail.com");

        // when: the request is sent to the service
        userOnboardApi.registerUser(request);

        // then: an email is sent to the user
        List<LoggedRequest> emailRequests = emailSim.verifyEmailSent("watson.phill@gmail.com",
            await().atMost(Duration.ofSeconds(30)));
        assertNotNull(emailRequests);
    }
}
