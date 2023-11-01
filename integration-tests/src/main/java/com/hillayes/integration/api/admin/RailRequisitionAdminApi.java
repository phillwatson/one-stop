package com.hillayes.integration.api.admin;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class RailRequisitionAdminApi extends ApiBase {
    private static final TypeRef<PaginatedList<Requisition>> PAGED_REQUISITIONS = new TypeRef<>() {};

    public RailRequisitionAdminApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedList<Requisition> list(int offset, int limit) {
        return givenAuth()
            .queryParam("offset", offset)
            .queryParam("limit", limit)
            .get("/api/v1/rails/admin/rail-requisitions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PAGED_REQUISITIONS);
    }

    public Requisition get(String requisitionId) {
        return get(requisitionId, 200)
            .as(Requisition.class);
    }

    public Response get(String requisitionId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/admin/rail-requisitions/{requisitionId}", requisitionId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }
}
