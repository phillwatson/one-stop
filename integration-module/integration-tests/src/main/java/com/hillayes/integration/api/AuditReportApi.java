package com.hillayes.integration.api;

import com.hillayes.onestop.api.AuditReportConfigRequest;
import com.hillayes.onestop.api.AuditReportConfigResponse;
import com.hillayes.onestop.api.PaginatedAuditConfigs;
import com.hillayes.onestop.api.PaginatedAuditTemplates;
import io.restassured.response.Response;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class AuditReportApi extends ApiBase {
    public AuditReportApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedAuditTemplates getAuditReportTemplates(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/audit/templates")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedAuditTemplates.class);
    }

    public PaginatedAuditConfigs getAuditReportConfigs(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/audit/configs")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedAuditConfigs.class);
    }

    public AuditReportConfigResponse getAuditReportConfig(UUID configId) {
        return getAuditReportConfig(configId, 200)
            .as(AuditReportConfigResponse.class);
    }

    public Response getAuditReportConfig(UUID configId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/audit/configs/{configId}", configId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public AuditReportConfigResponse createAuditReportConfig(AuditReportConfigRequest request) {
        return createAuditReportConfig(request, 201)
            .as(AuditReportConfigResponse.class);
    }

    public Response createAuditReportConfig(AuditReportConfigRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/rails/audit/configs")
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public AuditReportConfigResponse updateAuditReportConfig(UUID configId, AuditReportConfigRequest request) {
        return updateAuditReportConfig(configId, request, 200)
            .as(AuditReportConfigResponse.class);
    }

    public Response updateAuditReportConfig(UUID configId, AuditReportConfigRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/rails/audit/configs/{configId}", configId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public Response deleteAuditReportConfig(UUID configId) {
        return deleteAuditReportConfig(configId, 204);
    }

    public Response deleteAuditReportConfig(UUID configId, int expectedStatus) {
        return givenAuth()
            .delete("/api/v1/rails/audit/configs/{configId}", configId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }
}
