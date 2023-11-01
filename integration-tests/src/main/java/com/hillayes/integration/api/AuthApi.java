package com.hillayes.integration.api;

import com.hillayes.onestop.api.LoginRequest;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class AuthApi {
    public Map<String, String> login(String username, String password) {
        LoginRequest request = new LoginRequest()
            .username(username)
            .password(password);

        Response response = given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/auth/login")
            .then()
            .statusCode(204)
            .extract().response();

        return response.cookies();
    }

    public Map<String, String> logout(Map<String,String> cookies) {
        Response response = given()
            .cookies(cookies)
            .get("/api/v1/auth/refresh")
            .then()
            .statusCode(204)
            .extract().response();

        return response.cookies();
    }

    public Map<String, String> logout() {
        Response response = given()
            .get("/api/v1/auth/logout")
            .then()
            .statusCode(204)
            .extract().response();

        return response.cookies();
    }

    public String getJwkSet() {
        Response response = given()
            .get("/api/v1/auth/jwks.json")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().response();

        return response.asString();
    }
}
