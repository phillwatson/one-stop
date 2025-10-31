package com.hillayes.integration.api.rail;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.AccountResponse;
import com.hillayes.onestop.api.PaginatedAccounts;
import io.restassured.response.Response;

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
        return getAccount(accountId, 200)
            .as(AccountResponse.class);
    }

    public Response getAccount(UUID accountId, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .get("/api/v1/rails/accounts/{accountId}", accountId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }
}
