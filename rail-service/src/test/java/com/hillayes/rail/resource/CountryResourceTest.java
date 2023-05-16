package com.hillayes.rail.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class CountryResourceTest extends TestBase {
    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testListCountries() {
        given()
                .when().get("/api/v1/rails/countries")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("$.size()", equalTo(4))
                .extract().path("$");
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCountry() {
        given()
                .pathParam("id", "GB")
                .when().get("/api/v1/rails/countries/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("name", is("Great Britain"));
    }
}
