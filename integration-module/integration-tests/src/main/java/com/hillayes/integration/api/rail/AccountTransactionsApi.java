package com.hillayes.integration.api.rail;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.PaginatedTransactions;
import com.hillayes.onestop.api.TransactionResponse;
import io.restassured.common.mapper.TypeRef;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class AccountTransactionsApi extends ApiBase {
    private static final TypeRef<List<TransactionResponse>> TRANSACTION_LIST = new TypeRef<>() {};

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

    public TransactionResponse getTransaction(UUID transactionId) {
        return givenAuth()
            .get("/api/v1/rails/transactions/{transactionId}", transactionId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(TransactionResponse.class);
    }

    /**
     * Returns the authenticated user's transactions filtered by the identified category;
     * ordered by booking-datetime, descending.
     * If the given categoryId is null, then the un-categorised transactions are for
     * the identified category group returned.
     *
     * @param groupId the category group identifier.
     * @param categoryId the category identifier.
     * @param startDate the start date of the transactions (inclusive).
     * @param endDate the end date of the transactions (exclusive).
     * @return the transactions for the identified category.
     */
    public List<TransactionResponse> getTransactionsByCategory(UUID groupId, UUID categoryId,
                                                               Instant startDate, Instant endDate) {
        return givenAuth()
            .queryParam("categoryId", categoryId)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
            .get("/api/v1/rails/transactions/{groupId}/category", groupId)
            .then()
            .statusCode(200)
            .extract().as(TRANSACTION_LIST);
    }
}
