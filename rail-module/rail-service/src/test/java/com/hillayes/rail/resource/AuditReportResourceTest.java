package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.service.AccountTransactionService;
import com.hillayes.rail.service.AuditReportService;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.stream.IntStream;

import static com.hillayes.rail.utils.TestData.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AuditReportResourceTest extends TestBase {
    private static final TypeRef<List<AuditIssueSummaryResponse>> AUDIT_SUMMARIES_RESPONSE_LIST = new TypeRef<>() {};

    @InjectMock
    AuditReportService auditReportService;

    @InjectMock
    AccountTransactionService accountTransactionService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void getAuditTemplates() {
        // given: a page range
        int page = 0;
        int pageSize = 12;

        // and: a list of audit report templates
        List<AuditReportTemplate> templates = IntStream.range(0, 10)
            .mapToObj(i -> mockTemplate())
            .toList();
        when(auditReportService.getAuditTemplates(eq(page), eq(pageSize)))
            .thenReturn(Page.of(templates, page, pageSize));

        // when: client calls the endpoint
        PaginatedAuditTemplates response = given()
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

        // and: the page of templates is returned
        assertEquals(page, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(templates.size(), response.getCount());
        assertNotNull(response.getItems());
        templates.forEach(expected -> {
            AuditReportTemplateResponse actual = response.getItems().stream()
                .filter(t -> t.getName() != null && t.getName().equals(expected.getName()))
                .findFirst().orElse(null);
            assertNotNull(actual);
            assertEquals(expected.getDescription(), actual.getDescription());

            assertNotNull(actual.getParameters());
            assertEquals(expected.getParameters().size(), actual.getParameters().size());
            expected.getParameters().forEach(expectedParam -> {
                AuditReportParam actualParam = actual.getParameters().stream()
                    .filter(p -> p.getName().equals(expectedParam.name()))
                    .findFirst().orElse(null);
                assertNotNull(actualParam);
                assertEquals(expectedParam.description(), actualParam.getDescription());
                assertEquals(expectedParam.type().name(), actualParam.getType().name());
                assertEquals(expectedParam.defaultValue(), actualParam.getDefaultValue());
            });
        });
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

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetIssueSummaries() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a list of audit report configs
        List<AuditReportConfig> configs = IntStream.range(0, 5)
            .mapToObj(i -> mockAuditReportConfig(userId, r -> r.id(UUID.randomUUID())))
            .toList();

        // and: a list of audit issue summaries
        List<AuditIssueSummary> summaries = configs.stream()
            .map(TestData::mockAuditIssueSummary)
            .toList();

        // and: a list of audit issue summaries for the identified user
        when(auditReportService.getIssueSummaries(userId))
            .thenReturn(summaries);

        // when: client calls the endpoint
        List<AuditIssueSummaryResponse> response = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/summaries")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AUDIT_SUMMARIES_RESPONSE_LIST);

        // then: the auditReportService is called with the user id
        verify(auditReportService).getIssueSummaries(userId);

        // and: the response contains the summaries
        assertEquals(summaries.size(), response.size());
        summaries.forEach(expected -> {
            AuditIssueSummaryResponse actual = response.stream()
                .filter(r -> expected.getAuditConfigId().equals(r.getAuditConfigId()))
                .findFirst()
                .orElse(null);

            assertNotNull(actual);
            assertEquals(expected.getAuditConfigName(), actual.getAuditConfigName());
            assertEquals(expected.getTotalCount(), actual.getTotalCount());
            assertEquals(expected.getAcknowledgedCount(), actual.getAcknowledgedCount());
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { true, false })
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAuditIssues(Boolean acknowledged) {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a report config id
        UUID configId = UUID.randomUUID();

        // and: a collection of audit report issues exist
        List<AuditIssue> issues = IntStream.range(0, 35)
            .mapToObj(i -> mockAuditIssue(userId, configId))
            .toList();

        // and: a list of audit issue for the identified config
        when(auditReportService.getAuditIssues(any(), any(), nullable(Boolean.class), anyInt(), anyInt()))
            .then(invocation -> {
                int pageArg = invocation.getArgument(3);
                int pageSizeArg = invocation.getArgument(4);
                return Page.of(issues, pageArg, pageSizeArg);
            });

        // and: transactions exist for each issue transaction
        Account account = mockAccount(userId, UUID.randomUUID());
        when(accountTransactionService.listAll(any())).then(invocation -> {
            Collection<UUID> transactionIds = invocation.getArgument(0);
            return transactionIds.stream()
                .map(id -> mockAccountTransaction(account, t -> t.id(id)))
                .toList();
        });

        int page = 0;
        int pageSize = 10;
        int maxPages = (issues.size() / pageSize) + (issues.size() % pageSize == 0 ? 0 : 1);
        while (page < maxPages) {
            // when: client calls the endpoint
            PaginatedAuditIssues response = given()
                .request()
                .queryParam("page", page)
                .queryParam("page-size", pageSize)
                .queryParam("acknowledged", acknowledged)
                .contentType(JSON)
                .when()
                .get("/api/v1/rails/audit/configs/{configId}/issues", configId)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .as(PaginatedAuditIssues.class);

            // then: the auditReportService is called with the page range
            verify(auditReportService).getAuditIssues(userId, configId, acknowledged, page, pageSize);

            // and: the response contains the issues
            assertEquals(issues.size(), response.getTotal());
            assertEquals(page, response.getPage());
            assertEquals(maxPages, response.getTotalPages());

            // and: the page count is as expected
            int expectedPageSize = Math.min(pageSize, issues.size() - (page * pageSize));
            assertEquals(expectedPageSize, response.getCount());

            page++;
        }
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAuditIssues_NullAcknowledged() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a report config id
        UUID configId = UUID.randomUUID();

        // and: a page range
        int page = 1;
        int pageSize = 12;

        // and: an empty list of audit issue for the identified config
        when(auditReportService.getAuditIssues(any(), any(), nullable(Boolean.class), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // when: client calls the endpoint
        given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/configs/{configId}/issues", configId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedAuditIssues.class);

        // then: the auditReportService is called with the page range - and null acknowledged param
        verify(auditReportService).getAuditIssues(userId, configId, null, page, pageSize);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAuditIssue() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a transaction
        AccountTransaction transaction = mockAccountTransaction(
            mockAccount(userId, UUID.randomUUID())
        );
        when(accountTransactionService.getTransaction(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        // and: an audit report issues exist for the transaction
        AuditReportConfig reportConfig = mockAuditReportConfig(userId);
        AuditIssue issue = mockAuditIssue(reportConfig, i -> i
            .id(UUID.randomUUID())
            .transactionId(transaction.getId())
        );
        when(auditReportService.getAuditIssue(userId, issue.getId()))
            .thenReturn(issue);

        // when: client calls the endpoint
        AuditIssueResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/issues/{issueId}", issue.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AuditIssueResponse.class);

        // then: the audit report service was called
        verify(auditReportService).getAuditIssue(userId, issue.getId());

        // and: the identified issue is returned
        assertEquals(issue.getId(), response.getIssueId());
        assertEquals(reportConfig.getId(), response.getAuditConfigId());
        assertEquals(issue.isAcknowledged(), response.getAcknowledged());
        assertEquals(transaction.getId(), response.getId());
        assertEquals(transaction.getAccountId(), response.getAccountId());
        assertEquals(transaction.getTransactionId(), response.getTransactionId());
        assertEquals(transaction.getAmount().toDecimal(), response.getAmount());
        assertEquals(transaction.getAmount().getCurrencyCode(), response.getCurrency());
        assertEquals(transaction.getBookingDateTime(), response.getBookingDateTime());
        assertEquals(transaction.getValueDateTime(), response.getValueDateTime());
        assertEquals(transaction.getReference(), response.getReference());
        assertEquals(transaction.getAdditionalInformation(), response.getAdditionalInformation());
        assertEquals(transaction.getCreditorName(), response.getCreditorName());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAuditIssue_TransactionNotFound() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: an audit report issues exist
        AuditReportConfig reportConfig = mockAuditReportConfig(userId);
        AuditIssue issue = mockAuditIssue(reportConfig, i -> i
            .id(UUID.randomUUID())
            .transactionId(UUID.randomUUID())
        );
        when(auditReportService.getAuditIssue(userId, issue.getId()))
            .thenReturn(issue);

        // and: no transaction can be found
        when(accountTransactionService.getTransaction(issue.getTransactionId()))
            .thenReturn(Optional.empty());

        // when: client calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/audit/issues/{issueId}", issue.getId())
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: the audit report service was called
        verify(auditReportService).getAuditIssue(userId, issue.getId());

        // and: the response contains the expected error message
        assertNotNull(response);

        // and: the response contains an error message
        assertNotFoundError(response, (contextAttributes) -> {
            assertEquals("AccountTransaction", contextAttributes.get("entity-type"));
            assertEquals(issue.getTransactionId().toString(), contextAttributes.get("entity-id"));
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateAuditIssue() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a transaction
        AccountTransaction transaction = mockAccountTransaction(
            mockAccount(userId, UUID.randomUUID())
        );
        when(accountTransactionService.getTransaction(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        // and: an audit report issues exist for the transaction
        AuditReportConfig reportConfig = mockAuditReportConfig(userId);
        AuditIssue issue = mockAuditIssue(reportConfig, i -> i
            .id(UUID.randomUUID())
            .transactionId(transaction.getId())
        );
        when(auditReportService.updateIssue(eq(userId), eq(issue.getId()), anyBoolean()))
            .thenReturn(issue);

        // and: an update issue request
        AuditIssueRequest request = new AuditIssueRequest()
            .acknowledged(true);

        // when: client calls the endpoint
        AuditIssueResponse response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/audit/issues/{issueId}", issue.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(AuditIssueResponse.class);

        // then: the audit report service was called to update the issue
        verify(auditReportService).updateIssue(userId, issue.getId(), request.getAcknowledged());

        // and: the identified issue is returned
        assertEquals(issue.getId(), response.getIssueId());
        assertEquals(reportConfig.getId(), response.getAuditConfigId());
        assertEquals(issue.isAcknowledged(), response.getAcknowledged());
        assertEquals(transaction.getId(), response.getId());
        assertEquals(transaction.getAccountId(), response.getAccountId());
        assertEquals(transaction.getTransactionId(), response.getTransactionId());
        assertEquals(transaction.getAmount().toDecimal(), response.getAmount());
        assertEquals(transaction.getAmount().getCurrencyCode(), response.getCurrency());
        assertEquals(transaction.getBookingDateTime(), response.getBookingDateTime());
        assertEquals(transaction.getValueDateTime(), response.getValueDateTime());
        assertEquals(transaction.getReference(), response.getReference());
        assertEquals(transaction.getAdditionalInformation(), response.getAdditionalInformation());
        assertEquals(transaction.getCreditorName(), response.getCreditorName());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteAuditIssue() {
        // given: a user id
        UUID userId = UUID.fromString(userIdStr);

        // and: a transaction
        AccountTransaction transaction = mockAccountTransaction(
            mockAccount(userId, UUID.randomUUID())
        );
        when(accountTransactionService.getTransaction(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        // and: an audit report issues exist for the transaction
        AuditReportConfig reportConfig = mockAuditReportConfig(userId);
        AuditIssue issue = mockAuditIssue(reportConfig, i -> i
            .id(UUID.randomUUID())
            .transactionId(transaction.getId())
        );
        when(auditReportService.getAuditIssue(userId, issue.getId()))
            .thenReturn(issue);

        // when: client calls the endpoint
        given()
            .request()
            .contentType(JSON)
            .when()
            .delete("/api/v1/rails/audit/issues/{issueId}", issue.getId())
            .then()
            .statusCode(204);

        // then: the audit report service was called
        verify(auditReportService).deleteIssue(userId, issue.getId());
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

    private AuditReportTemplate mockTemplate() {
        AuditReportTemplate mock = mock(AuditReportTemplate.class);
        when(mock.getName()).thenReturn(randomAlphanumeric(20));
        when(mock.getDescription()).thenReturn(randomAlphanumeric(30));

        List<AuditReportTemplate.Parameter> parameters = List.of(
            new AuditReportTemplate.Parameter(
                randomAlphanumeric(20),
                randomAlphanumeric(20),
                AuditReportTemplate.ParameterType.STRING,
                randomAlphanumeric(20)
            )
        );
        when(mock.getParameters()).thenReturn(parameters);

        return mock;
    }
}
