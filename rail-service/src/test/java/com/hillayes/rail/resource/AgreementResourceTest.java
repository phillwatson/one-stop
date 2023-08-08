package com.hillayes.rail.resource;

import com.hillayes.rail.model.EndUserAgreementRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AgreementResourceTest extends TestResourceBase {
    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testFlow() {
        EndUserAgreementRequest agreementRequest = EndUserAgreementRequest.builder()
            .institutionId("SANDBOXFINANCE_SFIN0000")
            .accessScope(List.of("balances", "details", "transactions"))
            .accessValidForDays(10)
            .maxHistoricalDays(60)
            .build();

        // create agreement
        String agreementId = given()
            .request()
            .contentType(JSON)
            .body(agreementRequest)
            .when().post("/api/v1/rails/admin/rail-agreements")
            .then()
            .statusCode(201)
            .contentType(JSON)
            .body("max_historical_days", equalTo(agreementRequest.getMaxHistoricalDays()),
                "access_valid_for_days", equalTo(agreementRequest.getAccessValidForDays()),
                "access_scope", allOf(hasItem("balances"), hasItem("details"), hasItem("transactions")),
                "accepted", nullValue(),
                "institution_id", equalTo(agreementRequest.getInstitutionId()))
            .extract().path("id");

        // list all agreements
        given()
            .queryParam("limit", 100)
            .queryParam("offset", 0)
            .when().get("/api/v1/rails/admin/rail-agreements")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("count", equalTo(1),
                "results[0].id", equalTo(agreementId));

        // get agreement
        given()
            .pathParam("id", agreementId)
            .when().get("/api/v1/rails/admin/rail-agreements/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("max_historical_days", equalTo(agreementRequest.getMaxHistoricalDays()),
                "access_valid_for_days", equalTo(agreementRequest.getAccessValidForDays()),
                "access_scope", allOf(hasItem("balances"), hasItem("details"), hasItem("transactions")),
                "accepted", nullValue(),
                "institution_id", equalTo(agreementRequest.getInstitutionId()));

        // delete agreement
        given()
            .pathParam("id", agreementId)
            .when().delete("/api/v1/rails/admin/rail-agreements/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("summary", equalTo("End User Agreement deleted"));

        // get deleted agreement (should fail)
        given()
            .pathParam("id", agreementId)
            .when().get("/api/v1/rails/admin/rail-agreements/{id}")
            .then()
            .statusCode(404);
    }
}
