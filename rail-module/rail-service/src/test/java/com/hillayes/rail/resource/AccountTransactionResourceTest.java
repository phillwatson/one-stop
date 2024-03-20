package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.AccountTransactionService;
import com.hillayes.rail.repository.TransactionFilter;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import static org.apache.commons.lang3.RandomStringUtils.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AccountTransactionResourceTest extends TestBase {
    @InjectMock
    AccountTransactionService accountTransactionService;

    @InjectMock
    AccountService accountService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransactions() {
        UUID userId = UUID.fromString(userIdStr);

        // and: a page range
        int page = 1;
        int pageSize = 12;

        // and: a list of transactions
        when(accountTransactionService.getTransactions(eq(userId), any(), eq(page), eq(pageSize)))
            .thenReturn(Page.empty());

        // when: client calls the endpoint
        PaginatedTransactions response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/transactions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedTransactions.class);

        // then: the account-trans-service is called with the authenticated user-id and page
        verify(accountTransactionService).getTransactions(eq(userId), any(), eq(page), eq(pageSize));

        // and: no account look-up is performed
        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransactions_WithFilter() {
        UUID userId = UUID.fromString(userIdStr);
        UUID accountId = UUID.randomUUID();

        /// and: a page range
        int page = 1;
        int pageSize = 12;

        // and: a list of transactions
        when(accountTransactionService.getTransactions(eq(userId), any(), eq(page), eq(pageSize)))
            .thenReturn(Page.empty());

        // and: transaction filter properties
        LocalDate fromDate = LocalDate.now().minusDays(10);
        LocalDate toDate = LocalDate.now().minusDays(2);
        Float minAmount = 12.45f;
        Float maxAmount = 45.67f;
        String reference = randomAlphanumeric(10);
        String info = randomAlphanumeric(10);
        String creditor =randomAlphanumeric(10);

        // when: client calls the endpoint
        PaginatedTransactions response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .queryParam("account-id", accountId)
            .queryParam("from-date", fromDate.toString())
            .queryParam("to-date", toDate.toString())
            .queryParam("min-amount", minAmount)
            .queryParam("max-amount", maxAmount)
            .queryParam("reference", reference)
            .queryParam("info", info)
            .queryParam("creditor", creditor)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/transactions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedTransactions.class);

        // then: the account-trans-service is called with the authenticated user-id and page
        ArgumentCaptor<TransactionFilter> filterCaptor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(accountTransactionService).getTransactions(eq(userId), filterCaptor.capture(), eq(page), eq(pageSize));

        // and: the filter contains the account-id
        TransactionFilter capturedFilter = filterCaptor.getValue();
        assertNotNull(capturedFilter);
        assertEquals(accountId, capturedFilter.getAccountId());

        // and: the page links contain given filter properties
        PageLinks links = response.getLinks();
        assertTrue(links.getFirst().getQuery().contains("page=0"));
        assertTrue(links.getFirst().getQuery().contains("page-size=" + pageSize));
        assertTrue(links.getFirst().getQuery().contains("account-id=" + accountId));
        assertTrue(links.getFirst().getQuery().contains("from-date=" + fromDate));
        assertTrue(links.getFirst().getQuery().contains("to-date=" + toDate));
        assertTrue(links.getFirst().getQuery().contains("min-amount=" + minAmount));
        assertTrue(links.getFirst().getQuery().contains("max-amount=" + maxAmount));
        assertTrue(links.getFirst().getQuery().contains("reference=" + reference));
        assertTrue(links.getFirst().getQuery().contains("info=" + info));
        assertTrue(links.getFirst().getQuery().contains("creditor=" + creditor));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransaction_HappyPath() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an account transaction for the user can be identified
        AccountTransaction transaction = TestData.mockAccountTransaction(t -> t.userId(userId));
        when(accountTransactionService.getTransaction(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        // when: the client calls the endpoint
        TransactionSummaryResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("transactionId", transaction.getId())
            .get("/api/v1/rails/transactions/{transactionId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(TransactionSummaryResponse.class);

        // then: the identified transaction is returned
        assertNotNull(response);

        // and: the transaction is marshalled correctly
        assertEquals(transaction.getId(), response.getId());
        assertEquals(transaction.getAmount().toDecimal() , response.getAmount());
        assertEquals(transaction.getAmount().getCurrencyCode() , response.getCurrency());
        assertEquals(transaction.getBookingDateTime() , response.getDate());
        assertEquals(transaction.getReference() , response.getDescription());
        assertEquals(transaction.getAccountId() , response.getAccountId());

        // and: the service was called to retrieve the transaction
        verify(accountTransactionService).getTransaction(transaction.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransaction_TransactionNotFound() {
        // given: an account transaction for the user CANNOT be identified
        UUID transactionId = UUID.randomUUID();
        when(accountTransactionService.getTransaction(transactionId))
            .thenReturn(Optional.empty());

        // when: the client calls the endpoint
        // then: a 404 (not found) response is returned
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("transactionId", transactionId)
            .get("/api/v1/rails/transactions/{transactionId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: the service was called to retrieve the transaction
        verify(accountTransactionService).getTransaction(transactionId);

        // and: the response was returned
        assertNotNull(response);

        // and: the response contains the expected error message
        assertNotFoundError(response, (contextAttributes) -> {
            assertEquals("Transaction", contextAttributes.get("entity-type"));
            assertEquals(transactionId.toString(), contextAttributes.get("entity-id"));
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransaction_UserDoesNotHaveAccess() {
        // given: an account transaction not belonging to the caller
        AccountTransaction transaction = TestData.mockAccountTransaction(t -> t.userId(UUID.randomUUID()));
        when(accountTransactionService.getTransaction(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        // when: the client calls the endpoint
        // then: a 404 (not found) response is returned
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("transactionId", transaction.getId())
            .get("/api/v1/rails/transactions/{transactionId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // and: the response contains the expected error message
        assertNotNull(response);
        assertNotNull(response.getErrors());
        assertFalse(response.getErrors().isEmpty());

        ServiceError error = response.getErrors().get(0);
        assertEquals(ErrorSeverity.INFO, error.getSeverity());
        assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
        assertEquals("The identified entity cannot be found.", error.getMessage());

        // and: the transaction ID is given as context
        Map<String, String> contextAttributes = error.getContextAttributes();
        assertNotNull(contextAttributes);
        assertFalse(contextAttributes.isEmpty());
        assertEquals("Transaction", contextAttributes.get("entity-type"));
        assertEquals(transaction.getId().toString(), contextAttributes.get("entity-id"));

        // and: the service was called to retrieve the transaction
        verify(accountTransactionService).getTransaction(transaction.getId());
    }
}
