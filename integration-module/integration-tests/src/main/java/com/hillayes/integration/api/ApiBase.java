package com.hillayes.integration.api;

import io.restassured.specification.RequestSpecification;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;

public abstract class ApiBase {
    private final Map<String,String> authCookies;

    protected ApiBase(Map<String,String> authCookies) {
        this.authCookies = authCookies;
    }

    protected RequestSpecification givenAuth() {
        return given()
            .cookies(authCookies)
            .header("X-XSRF-TOKEN", authCookies.get("XSRF-TOKEN"));
    }

    public <T> T get(URI uri, Class<T> clazz) {
        return givenAuth().get(uri).as(clazz);
    }
}
