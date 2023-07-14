package com.hillayes.rail.resource;

import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.model.InstitutionDetail;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.AccountTransactionService;
import com.hillayes.rail.service.InstitutionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.rail.utils.TestData.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
