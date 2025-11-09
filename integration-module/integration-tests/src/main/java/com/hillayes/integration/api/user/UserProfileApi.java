package com.hillayes.integration.api.user;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.PasswordUpdateRequest;
import com.hillayes.onestop.api.UserAuthProvidersResponse;
import com.hillayes.onestop.api.UserProfileRequest;
import com.hillayes.onestop.api.UserProfileResponse;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class UserProfileApi extends ApiBase {
    public UserProfileApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public UserProfileResponse getProfile() {
        return givenAuth()
            .get("/api/v1/profiles")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserProfileResponse.class);
    }

    public UserAuthProvidersResponse getAuthProviders() {
        return givenAuth()
            .get("/api/v1/profiles/authproviders")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserAuthProvidersResponse.class);
    }

    public UserProfileResponse updateProfile(UserProfileRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/profiles")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(UserProfileResponse.class);
    }

    public void changePassword(PasswordUpdateRequest request) {
        changePassword(request, 204);
    }

    public Response changePassword(PasswordUpdateRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/profiles/password")
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }
}
