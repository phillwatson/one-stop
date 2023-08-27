package com.hillayes.integration.api;

import com.hillayes.onestop.api.PaginatedUsers;
import com.hillayes.onestop.api.UserResponse;
import com.hillayes.onestop.api.UserUpdateRequest;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class UserAdminApi {
    public static PaginatedUsers listUsers(String accessCookie) {
        return given()
            .cookie("access_token", accessCookie)
            .get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedUsers.class);
    }

    public static UserResponse getUser(String accessCookie,
                                       UUID userId) {
        return given()
            .cookie("access_token", accessCookie)
            .get("/api/v1/users/{userId}", userId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserResponse.class);
    }

    public static UserResponse updateUser(String accessCookie,
                                          UUID userId,
                                          UserUpdateRequest request) {
        return given()
            .cookie("access_token", accessCookie)
            .contentType(JSON)
            .body(request)
            .put("/api/v1/users/{userId}", userId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserResponse.class);
    }

    public static void deleteUser(String accessCookie,
                                  UUID userId) {
        given()
            .cookie("access_token", accessCookie)
            .delete("/api/v1/users/{userId}", userId)
            .then()
            .statusCode(204);
    }
}
