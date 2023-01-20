package com.hillayes.rail.resources;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class CountryResourceTest {
    @Test
    public void testListCountries() {
        List<Map<String,Object>> response = given()
                .when().get("/api/v1/countries")
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
    public void testGetCountry() {
        given()
                .pathParam("id", "GB")
                .when().get("/api/v1/countries/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("name", is("Great Britain"));
    }
}
