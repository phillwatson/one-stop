package com.hillayes.rail.resource;

import com.hillayes.rail.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class RequisitionResourceTest extends TestResourceBase {
    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testFlow() {
        List<?> institutions = given()
            .queryParam("country", "GB")
            .when().get("/api/v1/rails/banks")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().response().as(List.class);
        assertEquals(34, institutions.size());

        InstitutionDetail institution = given()
            .when().get("/api/v1/rails/banks/SANDBOXFINANCE_SFIN0000")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().response().as(InstitutionDetail.class);
        assertEquals("SANDBOXFINANCE_SFIN0000", institution.id);

        EndUserAgreementRequest agreementRequest = EndUserAgreementRequest.builder()
            .institutionId("SANDBOXFINANCE_SFIN0000")
            .accessScope(List.of("balances", "details", "transactions"))
            .accessValidForDays(10)
            .maxHistoricalDays(60)
            .build();
        EndUserAgreement mockAgreement = nordigenSimulator.stubAgreement(agreementRequest);

        // create agreement
        EndUserAgreement agreement = given()
            .request().contentType(JSON).body(agreementRequest)
            .when().post("/api/v1/rails/agreements")
            .then()
            .statusCode(201)
            .contentType(JSON)
            .extract().response().as(EndUserAgreement.class);
        assertEquals(mockAgreement, agreement);

        // get agreement
        given()
            .pathParam("id", agreement.id)
            .when().get("/api/v1/rails/agreements/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON);

        RequisitionRequest requisitionRequest = RequisitionRequest.builder()
            .agreement(agreement.id)
            .institutionId(agreement.institutionId)
            .redirect("http://localhost:8080/accepted")
            .reference(UUID.randomUUID().toString())
            .userLanguage("EN")
            .build();
        Requisition mockRequisition = nordigenSimulator.stubRequisition(requisitionRequest);

        // create requisition
        Requisition requisition = given()
            .request().contentType(JSON).body(requisitionRequest)
            .when().post("/api/v1/rails/requisitions")
            .then()
            .statusCode(201)
            .contentType(JSON)
            .body("redirect", equalTo(requisitionRequest.getRedirect()),
                "agreement", equalTo(requisitionRequest.getAgreement()),
                "institution_id", equalTo(requisitionRequest.getInstitutionId()),
                "reference", equalTo(requisitionRequest.getReference()),
                "user_language", equalTo(requisitionRequest.getUserLanguage()),
                "status", equalTo("CR"),
                "link", notNullValue())
            .extract().response().as(Requisition.class);
        assertEquals(mockRequisition, requisition);

        // list all requisitions
        given()
            .queryParam("limit", 100)
            .queryParam("offset", 0)
            .when().get("/api/v1/rails/requisitions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("count", equalTo(1),
                "results[0].id", equalTo(requisition.id));

        // get requisition
        String acceptanceLink = given()
            .pathParam("id", requisition.id)
            .when().get("/api/v1/rails/requisitions/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("redirect", equalTo(requisition.redirect),
                "agreement", equalTo(requisition.agreement),
                "institution_id", equalTo(requisition.institutionId),
                "reference", equalTo(requisition.reference),
                "user_language", equalTo(requisition.userLanguage),
                "status", equalTo("CR"))
            .extract().path("link");

        // delete the requisition
        given()
            .pathParam("id", requisition.id)
            .when().delete("/api/v1/rails/requisitions/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("summary", equalTo("Requisition deleted"));

        // get requisition again (should fail)
        given()
            .pathParam("id", requisition.id)
            .when().get("/api/v1/rails/requisitions/{id}")
            .then()
            .statusCode(404);

        // get agreement (should fail)
        given()
            .pathParam("id", requisition.agreement)
            .when().get("/api/v1/rails/agreements/{id}")
            .then()
            .statusCode(404);
    }
}
