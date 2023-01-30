package com.hillayes.user.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class UserResourceTest {
    @AfterEach
    public void cleanup() {
    }

    @Test
    public void testListUsers() {
        String response = given()
            .when().get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().asString();
        System.out.println(response);
    }
}
