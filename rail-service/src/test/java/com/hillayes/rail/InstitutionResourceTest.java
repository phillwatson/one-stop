package com.hillayes.rail;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class InstitutionResourceTest {
    @Test
    public void testListBanks() {
        List<Map<String,Object>> response = given()
                .queryParam("country", "GB")
                .queryParam("payments_enabled", false)
                .when().get("/banks")
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
    public void testGetBank() {
        given()
                .pathParam("id", "FIRST_DIRECT_MIDLGB22")
                .when().get("/banks/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("name", is("First Direct"));
    }
}
