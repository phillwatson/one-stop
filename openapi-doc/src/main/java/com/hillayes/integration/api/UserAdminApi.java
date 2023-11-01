package com.hillayes.integration.api;

import com.hillayes.onestop.api.PaginatedUsers;
import com.hillayes.onestop.api.UserResponse;
import com.hillayes.onestop.api.UserUpdateRequest;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class UserAdminApi extends ApiBase {
    public UserAdminApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedUsers listUsers(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedUsers.class);
    }

    public UserResponse getUser(UUID userId) {
        return givenAuth()
            .get("/api/v1/users/{userId}", userId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserResponse.class);
    }

    public UserResponse updateUser(UUID userId,
                                   UserUpdateRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/users/{userId}", userId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserResponse.class);
    }

    public void deleteUser(UUID userId) {
        givenAuth()
            .delete("/api/v1/users/{userId}", userId)
            .then()
            .statusCode(204);
    }
}
