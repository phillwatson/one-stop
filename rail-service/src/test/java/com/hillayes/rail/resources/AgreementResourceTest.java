package com.hillayes.rail.resources;

import com.hillayes.rail.services.model.EndUserAgreementRequest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AgreementResourceTest extends TestBase {
    @AfterEach
    public void cleanup() {
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
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .contentType(JSON)
                .body("max_historical_days", equalTo(agreement.getMaxHistoricalDays()),
                        "access_valid_for_days", equalTo(agreement.getAccessValidForDays()),
                        "access_scope", allOf(hasItem("balances"), hasItem("details"), hasItem("transactions")),
                        "accepted", nullValue(),
                        "institution_id", equalTo(agreement.getInstitutionId()))
                .extract().path("id");

        given()
                .queryParam("limit", 100)
                .queryParam("offset", 0)
                .when().get("/api/v1/agreements")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("count", equalTo(1),
                        "results[0].id", equalTo(agreementId));

        given()
                .pathParam("id", agreementId)
                .when().get("/api/v1/agreements/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("max_historical_days", equalTo(agreement.getMaxHistoricalDays()),
                        "access_valid_for_days", equalTo(agreement.getAccessValidForDays()),
                        "access_scope", allOf(hasItem("balances"), hasItem("details"), hasItem("transactions")),
                        "accepted", nullValue(),
                        "institution_id", equalTo(agreement.getInstitutionId()));

        given()
                .pathParam("id", agreementId)
                .when().delete("/api/v1/agreements/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("summary", equalTo("End User Agreement deleted"));

        given()
                .pathParam("id", agreementId)
                .when().get("/api/v1/agreements/{id}")
                .then()
                .statusCode(404);
    }
}
