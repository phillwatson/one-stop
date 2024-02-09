package com.hillayes.nordigen.resource;

import com.hillayes.nordigen.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RequisitionAdminResourceTest extends TestResourceBase {
    private static final TypeRef<List<Institution>> INSTITUTION_LIST = new TypeRef<>() {
    };

    @Test
    @TestSecurity(user = TestResourceBase.adminIdStr, roles = "admin")
    public void testFlow() {
        List<Institution> institutions = given()
            .queryParam("country", "GB")
            .when().get("/api/v1/rails/nordigen/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(INSTITUTION_LIST);
        assertFalse(institutions.isEmpty());

        InstitutionDetail institution = given()
            .when().get("/api/v1/rails/nordigen/institutions/SANDBOXFINANCE_SFIN0000")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().response().as(InstitutionDetail.class);
        assertEquals("SANDBOXFINANCE_SFIN0000", institution.id);

        EndUserAgreementRequest agreementRequest = EndUserAgreementRequest.builder()
            .institutionId(institution.id)
            .accessScope(List.of("balances", "details", "transactions"))
            .accessValidForDays(10)
            .maxHistoricalDays(institution.transactionTotalDays)
            .build();

        // create agreement
        EndUserAgreement agreement = given()
            .request().contentType(JSON).body(agreementRequest)
            .when().post("/api/v1/rails/nordigen/agreements")
            .then()
            .statusCode(201)
            .contentType(JSON)
            .extract().response().as(EndUserAgreement.class);

        // get agreement
        given()
            .pathParam("id", agreement.id)
            .when().get("/api/v1/rails/nordigen/agreements/{id}")
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

        // create requisition
        final Requisition requisition = given()
            .request().contentType(JSON).body(requisitionRequest)
            .when().post("/api/v1/rails/nordigen/requisitions")
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

        // list all requisitions
        given()
            .queryParam("limit", 100)
            .queryParam("offset", 0)
            .when().get("/api/v1/rails/nordigen/requisitions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("count", equalTo(1),
                "results[0].id", equalTo(requisition.id));

        // get requisition - until granted and linked to accounts
        AtomicReference<List<String>> accountIds = new AtomicReference<>();
        await().untilAsserted(() -> {
            Requisition req = given()
                .pathParam("id", requisition.id)
                .when().get("/api/v1/rails/nordigen/requisitions/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("redirect", equalTo(requisition.redirect),
                    "agreement", equalTo(requisition.agreement),
                    "institution_id", equalTo(requisition.institutionId),
                    "reference", equalTo(requisition.reference),
                    "user_language", equalTo(requisition.userLanguage))
                .extract().as(Requisition.class);
            assertEquals(RequisitionStatus.LN, req.status);
            accountIds.set(req.accounts);
        });

        assertFalse(accountIds.get().isEmpty());
        accountIds.get().forEach(accountId -> {
            // retrieve the account info
            AccountSummary account = given()
                .pathParam("id", accountId)
                .when().get("/api/v1/rails/nordigen/accounts/{id}")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(AccountSummary.class);
            assertEquals(requisition.institutionId, account.institutionId);

            // retrieve the account detail
            Map<?,?> detail = given()
                .pathParam("id", accountId)
                .when().get("/api/v1/rails/nordigen/accounts/{id}/details")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(Map.class);
            assertNotNull(detail);
            Map<String,Object> properties = (Map<String,Object>)detail.get("account");
            assertNotNull(properties);
            assertNotNull(properties.get("ownerName"));
            assertNotNull(properties.get("iban"));
            assertNotNull(properties.get("currency"));
            assertNotNull(properties.get("id"));

            // retrieve account balances
            List<Balance> balances = Arrays.asList(given()
                .pathParam("id", accountId)
                .when().get("/api/v1/rails/nordigen/accounts/{id}/balances")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(Balance[].class));
            assertNotNull(balances);
            assertFalse(balances.isEmpty());

            // retrieve account transactions
            TransactionList transactionsList = given()
                .pathParam("id", accountId)
                .when().get("/api/v1/rails/nordigen/accounts/{id}/transactions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(TransactionList.class);
            assertNotNull(transactionsList);
            assertNotNull(transactionsList.booked);
            assertFalse(transactionsList.booked.isEmpty());
            assertNotNull(transactionsList.pending);
            assertFalse(transactionsList.pending.isEmpty());
        });

        // delete the requisition
        given()
            .pathParam("id", requisition.id)
            .when().delete("/api/v1/rails/nordigen/requisitions/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("summary", equalTo("Requisition deleted"));

        // get requisition again (should fail)
        given()
            .pathParam("id", requisition.id)
            .when().get("/api/v1/rails/nordigen/requisitions/{id}")
            .then()
            .statusCode(404);

        // get agreement (should fail)
        given()
            .pathParam("id", requisition.agreement)
            .when().get("/api/v1/rails/admin/rail-agreements/{id}")
            .then()
            .statusCode(404);

        // get accounts (should fail)
        accountIds.get().forEach(accountId -> {
            given()
                .pathParam("id", accountId)
                .when().get("/api/v1/rails/nordigen/accounts/{id}")
                .then()
                .statusCode(404);
        });
    }
}
