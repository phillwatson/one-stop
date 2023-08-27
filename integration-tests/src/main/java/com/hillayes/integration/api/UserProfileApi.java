package com.hillayes.integration.api;

import com.hillayes.onestop.api.PasswordUpdateRequest;
import com.hillayes.onestop.api.UserAuthProvidersResponse;
import com.hillayes.onestop.api.UserProfileRequest;
import com.hillayes.onestop.api.UserProfileResponse;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class UserProfileApi {
    public static UserProfileResponse getProfile(String accessCookie) {
        return given()
            .cookie("access_token", accessCookie)
            .get("/api/v1/profiles")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserProfileResponse.class);
    }

    public static UserAuthProvidersResponse getAuthProviders(String accessCookie) {
        return given()
            .cookie("access_token", accessCookie)
            .get("/api/v1/profiles/authproviders")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserAuthProvidersResponse.class);
    }

    public static UserProfileResponse updateProfile(String accessCookie,
                                                    UserProfileRequest request) {
        return given()
            .cookie("access_token", accessCookie)
            .contentType(JSON)
            .body(request)
            .put("/api/v1/profiles")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserProfileResponse.class);
    }

    public static void changePassword(String accessCookie,
                                      PasswordUpdateRequest request) {
        given()
            .cookie("access_token", accessCookie)
            .contentType(JSON)
            .body(request)
            .put("/api/v1/profiles/password")
            .then()
            .statusCode(204);
    }
}
