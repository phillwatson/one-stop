package com.hillayes.rail.resource;

import com.hillayes.auth.xsrf.XsrfInterceptor;
import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.common.mapper.TypeRef;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

public class TestBase {
    protected static final String adminIdStr = "0945990c-13d6-4aad-8b67-29291c9ba717";
    protected static final String userIdStr = "0945990c-13d6-4aad-8b67-29291c9ba716";

    /**
     * Mocking the XsrfInterceptor to avoid the need to set the X-XSRF-TOKEN header in the tests.
     */
    @InjectMock
    XsrfInterceptor xsrfInterceptor;

    public void deleteAgreements() {
        PaginatedList<EndUserAgreement> agreements = given()
                .queryParam("limit", 100)
                .queryParam("offset", 0)
                .when().get("/api/v1/rails/agreements")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().response().as(new TypeRef<>() {});

        agreements.results.forEach(agreement -> {
            given()
                    .pathParam("id", agreement.id)
                    .when().delete("/api/v1/rails/agreements/{id}")
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
                .when().get("/api/v1/rails/requisitions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().response().as(new TypeRef<>() {});

        requisitions.results.forEach(agreement -> {
            given()
                    .pathParam("id", agreement.id)
                    .when().delete("/api/v1/rails/requisitions/{id}")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("summary", equalTo("Requisition deleted"));
        });
    }
}
