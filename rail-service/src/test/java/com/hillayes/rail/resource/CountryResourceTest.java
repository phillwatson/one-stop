package com.hillayes.rail.resource;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class CountryResourceTest extends TestBase {
    @InjectMock
    SecurityIdentity identity;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testListCountries() {
        List<Map<String,Object>> response = given()
                .when().get("/api/v1/rails/countries")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("$.size()", greaterThan(0))
                .extract().path("$");

        response.forEach(m ->
            System.out.println(m.get("id") + ": " + m.get("name"))
        );
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
