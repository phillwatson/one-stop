package com.hillayes.integration.test.audit;

import com.hillayes.integration.api.rail.AuditReportApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.onestop.api.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuditReportTestIT extends ApiTestBase {
    @Test
    public void testGetAuditReportTemplates() {
        // given: a user
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserEntity.builder()
            .username(randomStrings.nextAlphanumeric(20))
            .givenName(randomStrings.nextAlphanumeric(10))
            .password(randomStrings.nextAlphanumeric(30))
            .email(randomStrings.nextAlphanumeric(30))
            .build());

        AuditReportApi auditReportApi = new AuditReportApi(user.getAuthTokens());

        // when: the audit report templates are requested
        PaginatedAuditTemplates page = auditReportApi.getAuditReportTemplates(0, 10);

        // then: the response is not null and contains items
        assertNotNull(page);
        assertNotNull(page.getItems());
        assertFalse(page.getItems().isEmpty());
    }

    @Test
    public void testAuditReportConfigs() {
        // given: a user
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserEntity.builder()
            .username(randomStrings.nextAlphanumeric(20))
            .givenName(randomStrings.nextAlphanumeric(10))
            .password(randomStrings.nextAlphanumeric(30))
            .email(randomStrings.nextAlphanumeric(30))
            .build());

        AuditReportApi auditReportApi = new AuditReportApi(user.getAuthTokens());

        // when: the user requests their audit report configs
        PaginatedAuditConfigs userReportConfigs = auditReportApi.getAuditReportConfigs(0, 10);

        // then: the resulting page is empty
        assertNotNull(userReportConfigs);
        assertNotNull(userReportConfigs.getItems());
        assertTrue(userReportConfigs.getItems().isEmpty());
        assertEquals(0, userReportConfigs.getTotal());

        // given: the user retrieves an audit report template
        AuditReportTemplateResponse template = auditReportApi.getAuditReportTemplates(0, 10)
            .getItems().get(0);

        // and: the user creates an audit report config for that template
        AuditReportConfigRequest request = new AuditReportConfigRequest()
            .templateName(template.getName())
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50))
            .disabled(false)
            .source(AuditReportSource.ACCOUNT)
            .sourceId(UUID.randomUUID()); // this should identify a specific account

        // and: the user supplies config parameters for the report
        template.getParameters().forEach(param ->
            request.getParameters().put(param.getName(), switch (param.getType()) {
                    case STRING -> randomStrings.nextAlphanumeric(20);
                    case BOOLEAN -> "true";
                    case LONG -> "1";
                    case DOUBLE -> "1.0";
                }
            )
        );

        // when: the user submits the new config
        AuditReportConfigResponse config = auditReportApi.createAuditReportConfig(request);

        // then: the config is not null and has an id
        assertNotNull(config);
        assertNotNull(config.getId());

        // and: the new config has the same values as the original request
        assertEquals(request.getName(), config.getName());
        assertEquals(request.getDescription(), config.getDescription());
        assertEquals(request.getDisabled(), config.getDisabled());
        assertEquals(request.getTemplateName(), config.getTemplateName());
        assertEquals(request.getSource(), config.getSource());
        assertEquals(request.getSourceId(), config.getSourceId());
        assertEquals(request.getParameters(), config.getParameters());

        // when: the user requests their audit report configs
        userReportConfigs = auditReportApi.getAuditReportConfigs(0, 10);

        // then: the resulting page is not empty
        assertNotNull(userReportConfigs);
        assertNotNull(userReportConfigs.getItems());
        assertFalse(userReportConfigs.getItems().isEmpty());
        assertEquals(1, userReportConfigs.getTotal());

        // when: the user retrieves the config
        AuditReportConfigResponse retrievedConfig = auditReportApi.getAuditReportConfig(config.getId());

        // then: the retrieved config is not null and has the same id
        assertEquals(config.getId(), retrievedConfig.getId());

        // and: the retrieved config has the same values as the original config
        assertEquals(config.getName(), retrievedConfig.getName());
        assertEquals(config.getDescription(), retrievedConfig.getDescription());
        assertEquals(config.getDisabled(), retrievedConfig.getDisabled());
        assertEquals(config.getTemplateName(), retrievedConfig.getTemplateName());
        assertEquals(config.getSource(), retrievedConfig.getSource());
        assertEquals(config.getSourceId(), retrievedConfig.getSourceId());
        assertEquals(config.getParameters(), retrievedConfig.getParameters());

        // given: the user updates the config
        AuditReportConfigRequest updateRequest = new AuditReportConfigRequest()
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50))
            .disabled(false)
            .templateName(template.getName())
            .source(AuditReportSource.ACCOUNT);
        template.getParameters().forEach(param ->
            updateRequest.getParameters().put(param.getName(), switch (param.getType()) {
                    case STRING -> randomStrings.nextAlphanumeric(20);
                    case BOOLEAN -> "false";
                    case LONG -> "100";
                    case DOUBLE -> "123.4";
                }
            )
        );

        // when: the user submits the update
        AuditReportConfigResponse updatedConfig = auditReportApi.updateAuditReportConfig(config.getId(), updateRequest);

        // then: the updated config is not null and has the same id
        assertNotNull(updatedConfig);
        assertEquals(config.getId(), updatedConfig.getId());

        // and: the updated config has the same values as the update request
        assertEquals(updateRequest.getName(), updatedConfig.getName());
        assertEquals(updateRequest.getDescription(), updatedConfig.getDescription());
        assertEquals(updateRequest.getDisabled(), updatedConfig.getDisabled());
        assertEquals(updateRequest.getTemplateName(), updatedConfig.getTemplateName());
        assertEquals(updateRequest.getSource(), updatedConfig.getSource());
        assertEquals(updateRequest.getSourceId(), updatedConfig.getSourceId());
        assertEquals(updateRequest.getParameters(), updatedConfig.getParameters());

        // when: the user deletes the config
        auditReportApi.deleteAuditReportConfig(config.getId());

        // then: the config is no longer retrievable
        withServiceError(auditReportApi.getAuditReportConfig(config.getId(), 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("AuditReportConfig", error.getContextAttributes().get("entity-type"));
            assertEquals(config.getId().toString(), error.getContextAttributes().get("entity-id"));
        });
    }
}
