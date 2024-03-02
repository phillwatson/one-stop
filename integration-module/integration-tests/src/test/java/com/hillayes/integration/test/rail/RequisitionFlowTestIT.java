package com.hillayes.integration.test.rail;

import com.hillayes.integration.api.*;
import com.hillayes.integration.api.admin.RailAgreementAdminApi;
import com.hillayes.integration.api.admin.RailRequisitionAdminApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionStatus;
import com.hillayes.onestop.api.*;
import com.hillayes.sim.email.SendWithBlueSimulator;
import com.hillayes.sim.nordigen.NordigenSimClient;
import com.hillayes.sim.yapily.YapilySimClient;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class RequisitionFlowTestIT extends ApiTestBase {
    private static NordigenSimClient nordigenClient;
    private static YapilySimClient yapilyClient;

    private static Map<String, String> adminAuthTokens;

    @BeforeAll
    public static void initRailSim() {
        nordigenClient = newNordigenClient();
        yapilyClient = newYapilyClient();

        // the admin user signs in
        AuthApi authApi = new AuthApi();
        adminAuthTokens = authApi.login("admin", "password");
        assertNotNull(adminAuthTokens);
        assertEquals(3, adminAuthTokens.size());
    }

    @BeforeEach
    public void beforeEach() {
        nordigenClient.reset();
        yapilyClient.reset();
    }

    @Test
    public void testUserConsentEndToEnd() {
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
        AccountApi accountApi = new AccountApi(user.getAuthTokens());
        AccountTransactionsApi transactionsApi = new AccountTransactionsApi(user.getAuthTokens());

        // and: the user can identify the institution
        InstitutionResponse institution = institutionApi.getInstitution("SANDBOXFINANCE_SFIN0000");
        assertNotNull(institution);

        // when: the user initiates a consent request for the institution
        UserConsentRequest consentRequest = new UserConsentRequest()
            .callbackUri(URI.create("http://mock/callback/uri"));
        URI redirectUri = userConsentApi.register(institution.getId(), consentRequest);

        // then: the redirection to the rail registration is returned
        assertNotNull(redirectUri);
        assertTrue(redirectUri.getPath().endsWith(institution.getId()));

        // and: a user-consent record is created
        UserConsentResponse userConsent = userConsentApi.getConsentForInstitution(institution.getId());
        assertNotNull(userConsent);
        assertEquals(institution.getId(), userConsent.getInstitutionId());
        userConsent.institutionName(institution.getName());

        // and: the consent is waiting to be given
        assertEquals("WAITING", userConsent.getStatus());

        // and: an end-user agreement is created for the institution
        RailAgreementAdminApi agreementAdminApi = new RailAgreementAdminApi(adminAuthTokens);
        PaginatedList<EndUserAgreement> agreements = agreementAdminApi.list(0, 100);
        assertNotNull(agreements);
        assertEquals(1, agreements.count); // only one as we clear data on each test
        EndUserAgreement agreement = agreements.results.get(0);

        // and: the agreement references the institution
        assertEquals(institution.getId(), agreement.institutionId);

        // and: the consent scopes are correct
        assertEquals(3, agreement.accessScope.size());
        assertTrue(agreement.accessScope.contains("balances"));
        assertTrue(agreement.accessScope.contains("details"));
        assertTrue(agreement.accessScope.contains("transactions"));

        // and: a requisition record is created
        RailRequisitionAdminApi requisitionAdminApi = new RailRequisitionAdminApi(adminAuthTokens);
        PaginatedList<Requisition> requisitions = requisitionAdminApi.list(0, 100);
        assertNotNull(requisitions);
        assertEquals(1, requisitions.count); // only one as we clear data on each test
        Requisition requisition = requisitions.results.get(0);

        // and: the requisition status is "created"
        assertEquals(RequisitionStatus.CR, requisition.status);

        // and: the requisition references the agreement
        assertEquals(agreement.id, requisition.agreement);

        // and: the requisition references the institution
        assertEquals(institution.getId(), requisition.institutionId);

        // and: the requisition references the consent record
        assertNotNull(requisition.reference);

        // when: the requisition process is complete
        while (requisition.status != RequisitionStatus.LN) {
            requisition = requisitionAdminApi.get(requisition.id);
        }

        // then: the requisitioned accounts are identified
        assertFalse(requisition.accounts.isEmpty());

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            // when: the success response is returned from the rails service
            Response response = userConsentApi.consentResponse(institution.getProvider(), requisition.reference, null, null);

            // then: the redirect response is the original callback URI
            assertEquals(consentRequest.getCallbackUri().toString(), response.getHeader("Location"));

            // and: a confirmation email is sent to the user
            emailSim.verifyEmailSent(user.getEmail(), "Your OneStop access to " + institution.getName(),
                await().atMost(Duration.ofSeconds(60)));

            // and: the user can retrieve their consent record
            UserConsentResponse consentForInstitution = userConsentApi.getConsentForInstitution(institution.getId());
            assertNotNull(consentForInstitution);

            // and: consent status shows it has been given
            assertEquals("GIVEN", consentForInstitution.getStatus());

            // and: no error was recorded during consent
            assertNull(consentForInstitution.getErrorCode());
        }

        // when: the user's accounts have been polled by the service
        int accountCount = requisition.accounts.size();
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() ->
                assertEquals(accountCount, accountApi.getAccounts(0, 5).getCount())
            );

        // and: the user retrieves their consent record
        UserConsentResponse consentForInstitution = userConsentApi.getConsentForInstitution(institution.getId());

        // then: the consent shows the accounts
        assertNotNull(consentForInstitution.getAccounts());

        // when: the user retrieves their accounts
        consentForInstitution.getAccounts().forEach(accountSummary -> {
            // then: they all reference the selected institution
            assertEquals(institution.getId(), accountSummary.getInstitutionId());

            // when: the user retrieves their account transactions
            int pageSize = 5;
            PaginatedTransactions transactions =
                transactionsApi.getTransactions(0, pageSize, accountSummary.getId());

            // then: transactions are returned
            assertNotNull(transactions);
            assertEquals(0, transactions.getPage());
            assertEquals(pageSize, transactions.getPageSize());

            // and: the transactions reference the account
            assertNotNull(transactions.getItems());
            assertEquals(transactions.getCount(), transactions.getItems().size());
            transactions.getItems().forEach(transaction ->
                assertEquals(accountSummary.getId(), transaction.getAccountId())
            );

            int page = 0;
            long expectedTotalCount = transactions.getTotal();
            long totalCount = transactions.getCount();
            while (transactions.getLinks().getNext() != null) {
                // when: the next page is retrieved
                transactions = transactionsApi.get(transactions.getLinks().getNext(), PaginatedTransactions.class);

                // then: the next page is returned
                assertNotNull(transactions);
                assertEquals(++page, transactions.getPage());
                assertEquals(pageSize, transactions.getPageSize());
                assertEquals(expectedTotalCount, transactions.getTotal());

                // and: the transactions reference the account
                assertNotNull(transactions.getItems());
                assertEquals(transactions.getCount(), transactions.getItems().size());
                transactions.getItems().forEach(transaction ->
                    assertEquals(accountSummary.getId(), transaction.getAccountId())
                );

                totalCount += transactions.getCount();
            }

            // and: the total number of transactions is correct
            assertEquals(expectedTotalCount, totalCount);
        });

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            // when: the user deletes the consent
            userConsentApi.deleteConsent(institution.getId(), false);

            // then: an email is sent to the user for confirmation
            emailSim.verifyEmailSent(user.getEmail(), "Your OneStop access to " + institution.getName(),
                await().atMost(Duration.ofSeconds(60)));
        }

        // when: the user attempts to retrieve the institution consent
        withServiceError(userConsentApi.getConsentForInstitution(institution.getId(), 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("UserConsent", error.getContextAttributes().get("entity-type"));
        });

        // when: the user attempts to retrieve any account from the institution
        consentForInstitution.getAccounts().forEach(accountSummary -> {
            // then: a not-found error is returned
            withServiceError(accountApi.getAccount(accountSummary.getId(), 404), errorResponse -> {
                ServiceError error = errorResponse.getErrors().get(0);

                // then: a not-found error is returned
                assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
                assertNotNull(error.getContextAttributes());
                assertEquals("Account", error.getContextAttributes().get("entity-type"));
            });
        });

        // when: the user agreement is retrieved
        // then: a not-found error is returned
        agreementAdminApi.get(agreement.id, 404);

        // when: the requisition is retrieved
        // then: a not-found error is returned
        requisitionAdminApi.get(requisition.id, 404);
    }
}
