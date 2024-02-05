package com.hillayes.nordigen.resource;

import com.hillayes.nordigen.model.Institution;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class InstitutionResourceTest extends TestResourceBase {
    private static final TypeRef<List<Institution>> INSTITUTION_LIST = new TypeRef<>() {
    };

    @Test
    @TestSecurity(user = TestResourceBase.adminIdStr, roles = "admin")
    public void testListBanksPaymentNotEnabled() {
        List<Institution> response = given()
            .queryParam("country", "GB")
            .when().get("/api/v1/rails/nordigen/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(INSTITUTION_LIST);

        // and: the response corresponds to the expected list of banks
        assertEquals(34, response.size());
    }

    @Test
    @TestSecurity(user = TestResourceBase.adminIdStr, roles = "admin")
    public void testListBanksPaymentEnabled() {
        List<Institution> response = given()
            .queryParam("country", "GB")
            .queryParam("paymentsEnabled", true)
            .when().get("/api/v1/rails/nordigen/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(INSTITUTION_LIST);

        // and: the response corresponds to the expected list of banks
        assertEquals(74, response.size());
    }

    @Test
    @TestSecurity(user = TestResourceBase.adminIdStr, roles = "admin")
    public void testGetBank() {
        given()
            .pathParam("id", "FIRST_DIRECT_MIDLGB22")
            .when().get("/api/v1/rails/nordigen/institutions/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("name", is("First Direct"));
    }
}
