package com.hillayes.integration.api;

import com.hillayes.onestop.api.*;
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

    public PaginatedAuditIssues getAuditIssues(UUID configId) {
        return getAuditIssues(configId, 200)
            .as(PaginatedAuditIssues.class);
    }

    public Response getAuditIssues(UUID configId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/audit/configs/{configId}/issues")
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public AuditIssueResponse getAuditIssue(UUID issueId) {
        return getAuditIssue(issueId, 200)
            .as(AuditIssueResponse.class);
    }

    public Response getAuditIssue(UUID issueId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/audit/issues/{issueId}", issueId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public AuditIssueResponse updateAuditIssue(UUID issueId, AuditIssueRequest request) {
        return updateAuditIssue(issueId, request, 200)
            .as(AuditIssueResponse.class);
    }

    public Response updateAuditIssue(UUID issueId, AuditIssueRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/rails/audit/issues/{issueId}", issueId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public Response deleteAuditIssue(UUID issueId) {
        return deleteAuditIssue(issueId, 204);
    }

    public Response deleteAuditIssue(UUID issueId, int expectedStatus) {
        return givenAuth()
            .delete("/api/v1/rails/audit/issues/{issueId}", issueId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }
}
