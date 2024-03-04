package com.hillayes.integration.api;

import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionSummaryResponse;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class AccountTransactionsApi extends ApiBase {
    public AccountTransactionsApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedTransactions getTransactions(int pageIndex, int pageSize) {
        return getTransactions(pageIndex, pageSize, null);
    }

    public PaginatedTransactions getTransactions(int pageIndex, int pageSize, UUID accountId) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .queryParam("account-id", accountId)
            .get("/api/v1/rails/transactions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedTransactions.class);
    }

    public TransactionSummaryResponse getTransaction(UUID transactionId) {
        return givenAuth()
            .get("/api/v1/rails/transactions/{transactionId}", transactionId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(TransactionSummaryResponse.class);
    }
}
