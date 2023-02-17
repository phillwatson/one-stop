package com.hillayes.user.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class AuthResourceTest {
    @Test
    public void testGetJwkSet() {
        given()
            .when().get("/api/v1/auth/jwks.json")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .assertThat()
            .body("keys[0].kty", is("RSA"));
    }
}
