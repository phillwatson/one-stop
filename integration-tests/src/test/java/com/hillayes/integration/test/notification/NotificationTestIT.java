package com.hillayes.integration.test.notification;

import com.hillayes.integration.api.AuthApi;
import com.hillayes.integration.api.InstitutionApi;
import com.hillayes.integration.api.NotificationApi;
import com.hillayes.integration.api.UserConsentApi;
import com.hillayes.integration.api.admin.RailRequisitionAdminApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionStatus;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.NotificationResponse;
import com.hillayes.onestop.api.UserConsentRequest;
import com.hillayes.sim.email.SendWithBlueSimulator;
import com.hillayes.sim.nordigen.NordigenSimClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationTestIT extends ApiTestBase {
    private static NordigenSimClient railClient;

    private static Map<String, String> adminAuthTokens;

    @BeforeAll
    public static void initRailSim() {
        railClient = newRailClient();

        // the admin user signs in
        AuthApi authApi = new AuthApi();
        adminAuthTokens = authApi.login("admin", "password");
        assertNotNull(adminAuthTokens);
        assertEquals(3, adminAuthTokens.size());
    }

    @BeforeEach
    public void beforeEach() {
        railClient.reset();
    }

    @Test
    public void testUserConsentNotification() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        // establish authenticated APIs
        InstitutionApi institutionApi = new InstitutionApi(user.getAuthTokens());
        UserConsentApi userConsentApi = new UserConsentApi(user.getAuthTokens());
        NotificationApi notificationApi = new NotificationApi(user.getAuthTokens());
        Instant testStarted = Instant.now();

        // and: the user registers for institution consent
        InstitutionResponse institution = institutionApi.getInstitution("SANDBOXFINANCE_SFIN0000");
        UserConsentRequest consentRequest = new UserConsentRequest()
            .callbackUri(URI.create("http://mock/callback/uri"));
        userConsentApi.register(institution.getId(), consentRequest);

        // and: the requisition process is complete
        RailRequisitionAdminApi requisitionAdminApi = new RailRequisitionAdminApi(adminAuthTokens);
        PaginatedList<Requisition> requisitions = requisitionAdminApi.list(0, 100);
        Requisition requisition = requisitions.results.get(0);
        while (requisition.status != RequisitionStatus.LN) {
            requisition = requisitionAdminApi.get(requisition.id);
        }

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            // when: an error response is returned from the rails service
            String errorDetails = randomAlphanumeric(30);
            userConsentApi.consentResponse(requisition.reference, "mock-error-code", errorDetails);

            // then: a confirmation email is sent to the user
            emailSim.verifyEmailSent(user.getEmail(), "Your OneStop access to " + institution.getName(),
                await().atMost(Duration.ofSeconds(60)));

            // and: a notification is issued to the user
            List<NotificationResponse> notifications = notificationApi.getNotifications(testStarted);
            assertNotNull(notifications);
            assertEquals(1, notifications.size());

            // and: the notification shows the reason for denial
            NotificationResponse notification = notifications.get(0);
            assertEquals("CONSENT", notification.getTopic());
            assertTrue(notification.getMessage().contains("Reason given '" + errorDetails + "'"));
        }
   }
}
