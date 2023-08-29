package com.hillayes.integration.api;

import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class UserOnboardApi extends ApiBase {
    public UserOnboardApi() {
        super(null);
    }

    public void registerUser(UserRegisterRequest request) {
        given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/users/onboard/register")
            .then()
            .statusCode(202);
    }

    public Map<String,String> onboardUser(UserCompleteRequest request) {
        Response response = given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/users/onboard/complete")
            .then()
            .statusCode(201)
            .extract().response();

        return response.cookies();
    }
}
