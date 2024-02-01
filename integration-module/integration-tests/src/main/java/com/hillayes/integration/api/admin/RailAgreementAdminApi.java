package com.hillayes.integration.api.admin;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.PaginatedList;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class RailAgreementAdminApi extends ApiBase {
    private static final TypeRef<PaginatedList<EndUserAgreement>> PAGED_AGREEMENTS = new TypeRef<>() {};

    public RailAgreementAdminApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedList<EndUserAgreement> list(int offset, int limit) {
        return givenAuth()
            .queryParam("offset", offset)
            .queryParam("limit", limit)
            .get("/api/v1/rails/nordigen/agreements")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PAGED_AGREEMENTS);
    }

    public EndUserAgreement get(String agreementId) {
        return get(agreementId, 200)
            .as(EndUserAgreement.class);
    }

    public Response get(String agreementId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/nordigen/agreements/{agreementId}", agreementId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }
}
