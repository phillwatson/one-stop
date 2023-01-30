package com.hillayes.rail.resource;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import io.restassured.common.mapper.TypeRef;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

public class TestBase {
    public void deleteAgreements() {
        PaginatedList<EndUserAgreement> agreements = given()
                .queryParam("limit", 100)
                .queryParam("offset", 0)
                .when().get("/agreements")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().response().as(new TypeRef<>() {});

        agreements.results.forEach(agreement -> {
            given()
                    .pathParam("id", agreement.id)
                    .when().delete("/agreements/{id}")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("summary", equalTo("End User Agreement deleted"));
        });
    }

    public void deleteRequisitions() {
        PaginatedList<Requisition> requisitions = given()
                .queryParam("limit", 100)
                .queryParam("offset", 0)
                .when().get("/requisitions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().response().as(new TypeRef<>() {});

        requisitions.results.forEach(agreement -> {
            given()
                    .pathParam("id", agreement.id)
                    .when().delete("/requisitions/{id}")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("summary", equalTo("Requisition deleted"));
        });
    }
}
