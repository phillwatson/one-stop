package com.hillayes.rail.resource;

import com.hillayes.onestop.api.PaginatedAccounts;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
public class AccountResourceTest {
    @TestSecurity(user = "0945990c-13d6-4aad-8b67-29291c9ba716", roles = "user")
    @Test
    public void testGetAccounts() {
        PaginatedAccounts response = given()
            .request()
            .queryParam("page", 0)
            .queryParam("page-size", 20)
            .contentType(JSON)

            .when()
            .get("/api/v1/rails/accounts")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedAccounts.class);

        System.out.println(response);
    }



}
