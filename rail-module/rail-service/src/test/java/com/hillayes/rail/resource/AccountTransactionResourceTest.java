package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.PageLinks;
import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionList;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.AccountTransactionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AccountTransactionResourceTest extends TestBase {
    @InjectMock
    AccountTransactionService accountTransactionService;

    @InjectMock
    AccountService accountService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransactionsForDateRange() {
        UUID userId = UUID.fromString(userIdStr);

        // and: a date range for the transactions
        LocalDate fromDate = LocalDate.now().minusDays(30);
        LocalDate toDate = LocalDate.now();

        // when: client calls the endpoint
        TransactionList response = given()
            .request()
            .queryParam("from-date", fromDate.toString())
            .queryParam("to-date", toDate.toString())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/transactions/dates")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(TransactionList.class);

        // then: the account-trans-service is called with the authenticated user-id, and date range
        verify(accountTransactionService).listTransactions(userId, null, fromDate, toDate);

        // and: no account look-up is performed
        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransactions() {
        UUID userId = UUID.fromString(userIdStr);

        /// and: a page range
        int page = 1;
        int pageSize = 12;

        // and: a list of transactions
        when(accountTransactionService.getTransactions(userId, null, page, pageSize))
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
        verify(accountTransactionService).getTransactions(userId, null, page, pageSize);

        // and: no account look-up is performed
        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetTransactions_WithUserId() {
        UUID userId = UUID.fromString(userIdStr);

        /// and: a page range
        int page = 1;
        int pageSize = 12;

        // and: a list of transactions
        when(accountTransactionService.getTransactions(userId, userId, page, pageSize))
            .thenReturn(Page.empty());

        // when: client calls the endpoint
        PaginatedTransactions response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .queryParam("account-id", userId)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/transactions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedTransactions.class);

        // then: the account-trans-service is called with the authenticated user-id and page
        verify(accountTransactionService).getTransactions(userId, userId, page, pageSize);

        // and: no account look-up is performed
        verifyNoInteractions(accountService);

        // and: the page links contain given parameters
        PageLinks links = response.getLinks();
        assertTrue(links.getFirst().getQuery().contains("account-id=" + userId));
        assertTrue(links.getFirst().getQuery().contains("page=" + 0));
        assertTrue(links.getFirst().getQuery().contains("page-size=" + pageSize));
    }
}
