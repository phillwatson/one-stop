package com.hillayes.rail.audit;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class AuditReportTemplateTest {
    @Inject
    @Any
    Instance<AuditReportTemplate> reportTemplates;

    @Test
    public void testReportTemplates() {
        reportTemplates.forEach(reportTemplate -> {
            assertNotNull(reportTemplate.getName());
            assertNotNull(reportTemplate.getDescription());
            assertNotNull(reportTemplate.getParameters());
        });
    }
}
