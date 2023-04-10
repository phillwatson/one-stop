package com.hillayes.user.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
public class UserAdminResourceTest {
    private static final String userIdStr = "0945990c-13d6-4aad-8b67-29291c9ba716";

    @Test
    @TestSecurity(user = userIdStr, roles = "admin")
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
