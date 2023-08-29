package com.hillayes.integration.api;

import com.hillayes.onestop.api.AccountResponse;
import com.hillayes.onestop.api.PaginatedAccounts;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class AccountApi extends ApiBase {
    public AccountApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedAccounts getAccounts(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/accounts")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedAccounts.class);
    }

    public AccountResponse getAccount(UUID accountId) {
        return givenAuth()
            .contentType(JSON)
            .get("/api/v1/rails/accounts/{accountId}", accountId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(AccountResponse.class);
    }
}
