package com.hillayes.rail;

import com.hillayes.rail.services.model.*;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class RequisitionResourceTest extends TestBase {
    @AfterEach
    public void cleanup() {
        deleteRequisitions();
        deleteAgreements();
    }

    @Test
    public void testFlow() {
        EndUserAgreementRequest agreement = EndUserAgreementRequest.builder()
                .institutionId("SANDBOXFINANCE_SFIN0000")
                .accessScope(List.of("balances", "details", "transactions"))
                .accessValidForDays(10)
                .maxHistoricalDays(60)
                .build();

        String agreementId = given()
                .request()
                .contentType(JSON)
                .body(agreement)
                .when().post("/agreements")
                .then()
                .statusCode(201)
                .contentType(JSON)
                .extract().path("id");

        RequisitionRequest requisition = RequisitionRequest.builder()
                .institutionId(agreement.getInstitutionId())
                .agreement(UUID.fromString(agreementId))
                .redirect("http://localhost:8080/accepted")
                .reference(UUID.randomUUID().toString())
                .userLanguage("EN")
                .build();

        String requisitionId = given()
                .request()
                .contentType(JSON)
                .body(requisition)
                .when().post("/requisitions")
                .then()
                .statusCode(201)
                .contentType(JSON)
                .body("redirect", equalTo(requisition.getRedirect()),
                        "agreement", equalTo(requisition.getAgreement().toString()),
                        "institution_id", equalTo(requisition.getInstitutionId()),
                        "reference", equalTo(requisition.getReference()),
                        "user_language", equalTo(requisition.getUserLanguage()),
                        "status", equalTo("CR"),
                        "link", notNullValue())
                .extract().path("id");

        given()
                .queryParam("limit", 100)
                .queryParam("offset", 0)
                .when().get("/requisitions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("count", equalTo(1),
                        "results[0].id", equalTo(requisitionId));

        String acceptanceLink = given()
                .pathParam("id", requisitionId)
                .when().get("/requisitions/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("redirect", equalTo(requisition.getRedirect()),
                        "agreement", equalTo(requisition.getAgreement().toString()),
                        "institution_id", equalTo(requisition.getInstitutionId()),
                        "reference", equalTo(requisition.getReference()),
                        "user_language", equalTo(requisition.getUserLanguage()),
                        "status", equalTo("CR"))
                .extract().path("link");

        given()
                .pathParam("id", requisitionId)
                .when().delete("/requisitions/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("summary", equalTo("Requisition deleted"));

        given()
                .pathParam("id", requisitionId)
                .when().get("/requisitions/{id}")
                .then()
                .statusCode(404);
    }


}
