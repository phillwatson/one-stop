package com.hillayes.integration.api;

import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public abstract class ApiBase {
    private Map<String,String> authCookies;

    ApiBase(Map<String,String> authCookies) {
        this.authCookies = authCookies;
    }

    protected RequestSpecification givenAuth() {
        return given()
            .cookies(authCookies)
            .header("X-XSRF-TOKEN", authCookies.get("XSRF-TOKEN"));
    }
}
