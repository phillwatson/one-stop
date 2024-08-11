package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.service.AuditReportService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static com.hillayes.rail.utils.TestData.mockAuditReportConfig;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AuditReportResourceTest extends TestBase {
    @InjectMock
    AuditReportService auditReportService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void getAuditTemplates() {
        // given: a page range
        int page = 1;
        int pageSize = 12;

        // and: a list of audit report templates
        when(auditReportService.getAuditTemplates(eq(page), eq(pageSize)))
            .thenReturn(Page.empty());

        // when: client calls the endpoint
        given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/templates")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedAuditTemplates.class);

        // then: the auditReportService is called with the page range
        verify(auditReportService).getAuditTemplates(page, pageSize);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAuditConfigs() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a page range
        int page = 1;
        int pageSize = 12;

        // and: a list of audit report configs
        when(auditReportService.getAuditConfigs(userId, page, pageSize))
            .thenReturn(Page.empty());

        // when: client calls the endpoint
        given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/configs")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedTransactions.class);

        // then: the auditReportService is called with the page range
        verify(auditReportService).getAuditConfigs(userId, page, pageSize);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreateAuditConfig() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: an audit report config
        AuditReportConfigRequest request = mockRequest();

        // and: a created audit report config
        AuditReportConfig config = mockConfig(userId, request);
        when(auditReportService.createAuditConfig(eq(userId), any()))
            .thenReturn(config);

        // when: client calls the endpoint
        Response response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/rails/audit/configs")
            .then()
            .statusCode(201)
            .contentType(JSON)
            .extract().response();

        // then: the auditReportService is called with the user id and config
        ArgumentCaptor<AuditReportConfig> captor = ArgumentCaptor.forClass(AuditReportConfig.class);
        verify(auditReportService).createAuditConfig(eq(userId), captor.capture());

        // and: the audit report config is created with the request parameters
        AuditReportConfig created = captor.getValue();
        assertEquals(request.getName(), created.getName());
        assertEquals(request.getDescription(), created.getDescription());
        assertEquals(request.getSource().name(), created.getReportSource().name());
        assertEquals(request.getSourceId(), created.getReportSourceId());
        assertEquals(request.getUncategorisedIncluded(), created.isUncategorisedIncluded());
        assertEquals(request.getTemplateName(), created.getTemplateName());
        assertEquals(request.getParameters().size(), created.getParameters().size());

        // and: the new audit report config is returned
        AuditReportConfigResponse configResponse = response.as(AuditReportConfigResponse.class);
        assertEquals(config.getId(), configResponse.getId());
        assertEquals(config.getName(), configResponse.getName());

        // and: the config location is returned in the header
        String location = response.getHeader("Location");
        assertTrue(location.endsWith("/api/v1/rails/audit/configs/" + config.getId()));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAuditConfig() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: an audit report config id
        UUID configId = UUID.randomUUID();

        // and: an audit report config exists
        AuditReportConfig config = mockAuditReportConfig(userId, b -> b.id(configId));
        when(auditReportService.getAuditConfig(userId, configId))
            .thenReturn(config);

        // when: client calls the endpoint
        AuditReportConfigResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/configs/{configId}", configId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AuditReportConfigResponse.class);

        // then: the auditReportService is called with the user id and config id
        verify(auditReportService).getAuditConfig(userId, configId);

        // and: the audit report config is returned
        assertEquals(config.getId(), response.getId());
        assertEquals(config.getName(), response.getName());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateAuditConfig() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: an audit report config id
        UUID configId = UUID.randomUUID();

        // and: an audit report config request
        AuditReportConfigRequest request = mockRequest();

        // and: an updated audit report config
        AuditReportConfig config = mockConfig(userId, request);
        when(auditReportService.updateAuditConfig(eq(userId), eq(configId), any()))
            .thenReturn(config);

        // when: client calls the endpoint
        AuditReportConfigResponse response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/audit/configs/{configId}", configId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AuditReportConfigResponse.class);

        // then: the auditReportService is called with the user id, config id and config
        ArgumentCaptor<AuditReportConfig> captor = ArgumentCaptor.forClass(AuditReportConfig.class);
        verify(auditReportService).updateAuditConfig(eq(userId), eq(configId), captor.capture());

        // and: the audit report config is created with the request parameters
        AuditReportConfig updated = captor.getValue();
        assertEquals(request.getName(), updated.getName());
        assertEquals(request.getDescription(), updated.getDescription());
        assertEquals(request.getSource().name(), updated.getReportSource().name());
        assertEquals(request.getSourceId(), updated.getReportSourceId());
        assertEquals(request.getUncategorisedIncluded(), updated.isUncategorisedIncluded());
        assertEquals(request.getTemplateName(), updated.getTemplateName());
        assertEquals(request.getParameters().size(), updated.getParameters().size());

        // and: the updated audit report config is returned
        assertEquals(config.getId(), response.getId());
        assertEquals(config.getName(), response.getName());
    }


    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteAuditConfig() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: an audit report config id
        UUID configId = UUID.randomUUID();

        // when: client calls the endpoint
        given()
            .request()
            .contentType(JSON)
            .when()
            .delete("/api/v1/rails/audit/configs/{configId}", configId)
            .then()
            .statusCode(204);

        // then: the auditReportService is called with the user id and config id
        verify(auditReportService).deleteAuditConfig(userId, configId);
    }

    private AuditReportConfigRequest mockRequest() {
        return new AuditReportConfigRequest()
            .disabled(false)
            .templateName(randomAlphanumeric(30))
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(30))
            .source(AuditReportSource.CATEGORY_GROUP)
            .sourceId(UUID.randomUUID())
            .uncategorisedIncluded(true)
            .parameters(Map.of(
                randomAlphanumeric(20), randomAlphanumeric(20),
                randomAlphanumeric(20), randomAlphanumeric(20)
            ));
    }

    private AuditReportConfig mockConfig(UUID userId, AuditReportConfigRequest request) {
        AuditReportConfig result = mockAuditReportConfig(userId, b -> b
            .id(UUID.randomUUID())
            .userId(userId)
            .disabled(request.getDisabled() != null && request.getDisabled())
            .name(request.getName())
            .description(request.getDescription())
            .reportSource(AuditReportConfig.ReportSource.valueOf(request.getSource().name()))
            .reportSourceId(request.getSourceId())
            .uncategorisedIncluded(request.getUncategorisedIncluded() != null && request.getUncategorisedIncluded())
            .templateName(request.getTemplateName())
        );

        request.getParameters().forEach(result::addParameter);
        return result;
    }
}
