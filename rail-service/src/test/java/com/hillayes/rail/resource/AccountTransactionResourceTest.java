package com.hillayes.rail.resource;

import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionList;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.AccountTransactionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
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
        verify(accountTransactionService).getTransactions(userId, null, fromDate, toDate);

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
            .thenReturn(new PageImpl<>(List.of()));

        // when: client calls the endpoint
        PaginatedTransactions response = given()
            .request()
            .queryParam("page", 1)
            .queryParam("page-size", 12)
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
}
