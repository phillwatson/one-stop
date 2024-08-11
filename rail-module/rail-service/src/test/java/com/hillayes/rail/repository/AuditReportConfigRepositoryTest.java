package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.domain.AuditReportParameter;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.rail.utils.TestData.mockAuditReportConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class AuditReportConfigRepositoryTest {
    @Inject
    AuditReportConfigRepository fixture;

    @Test
    public void testInsertReport() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an audit report config is created
        AuditReportConfig auditReport = mockAuditReportConfig(userId);

        // when: the audit report config is saved
        AuditReportConfig savedReport = fixture.save(auditReport);
        fixture.flush();

        // then: the saved report is not null
        assertNotNull(savedReport);
        assertNotNull(savedReport.getId());

        // and: the saved report has the same identity
        assertEquals(auditReport.isDisabled(), savedReport.isDisabled());
        assertEquals(auditReport.getUserId(), savedReport.getUserId());
        assertEquals(auditReport.getName(), savedReport.getName());
        assertEquals(auditReport.getDescription(), savedReport.getDescription());
        assertEquals(auditReport.getReportSource(), savedReport.getReportSource());
        assertEquals(auditReport.getReportSourceId(), savedReport.getReportSourceId());
        assertEquals(auditReport.isUncategorisedIncluded(), savedReport.isUncategorisedIncluded());
        assertEquals(auditReport.getTemplateName(), savedReport.getTemplateName());
    }

    @Test
    public void testReportConfig() {
        // given: an audit report config is created
        AuditReportConfig auditReport = mockAuditReportConfig(UUID.randomUUID());

        // when: the report parameters are added
        auditReport
            .addParameter("key1", "value1")
            .addParameter("key2", "value2");

        // then: the report has the expected number of parameters
        assertEquals(2, auditReport.getParameters().size());

        // and: the parameters are as expected
        assertEquals("value1", auditReport.getParameters().get("key1").getValue());
        assertEquals("value2", auditReport.getParameters().get("key2").getValue());

        // when: the report parameters are updated
        auditReport.addParameter("key1", "updated value1");
        auditReport.addParameter("key2", "updated value2");
        auditReport.addParameter("key3", "updated value3");

        // then: the report has the expected number of parameters
        assertEquals(3, auditReport.getParameters().size());

        // and: the parameters are as expected
        assertEquals("updated value1", auditReport.getParameters().get("key1").getValue());
        assertEquals("updated value2", auditReport.getParameters().get("key2").getValue());
        assertEquals("updated value3", auditReport.getParameters().get("key3").getValue());
    }

    @Test
    public void testInsertReportWithConfig() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an audit report config is created
        AuditReportConfig auditReport = mockAuditReportConfig(userId);

        // and: the report has parameters
        auditReport
            .addParameter("key1", "value1")
            .addParameter("key2", "value2");
        assertEquals(2, auditReport.getParameters().size());

        // when: the audit report config is saved
        AuditReportConfig savedReport = fixture.save(auditReport);
        fixture.flush();

        // then: the saved report is not null
        assertNotNull(savedReport);
        assertNotNull(savedReport.getId());

        // and: the saved report has the same user identity
        assertEquals(auditReport.getUserId(), savedReport.getUserId());
        assertEquals(auditReport.getName(), savedReport.getName());
        assertEquals(auditReport.getDescription(), savedReport.getDescription());
        assertEquals(auditReport.getTemplateName(), savedReport.getTemplateName());

        // and: the parameters are saved
        assertEquals(auditReport.getParameters().size(), savedReport.getParameters().size());
        auditReport.getParameters().forEach((key, expectedValue) -> {
            AuditReportParameter actual = savedReport.getParameters().get(key);

            assertNotNull(actual.getId());
            assertNotNull(actual.getConfig());
            assertEquals(expectedValue.getValue(), actual.getValue());
        });

        // clear the entity cache
        fixture.getEntityManager().clear();

        // when: the report is retrieved
        AuditReportConfig retrievedReport = fixture.findByIdOptional(savedReport.getId())
            .orElse(null);

        // then: the retrieved report is not null
        assertNotNull(retrievedReport);

        // and: the parameters are retrieved
        assertEquals(auditReport.getParameters().size(), retrievedReport.getParameters().size());
        auditReport.getParameters().forEach((key, expectedValue) -> {
            AuditReportParameter actual = savedReport.getParameters().get(key);
            assertNotNull(actual);
            assertEquals(expectedValue.getValue(), actual.getValue());
        });
    }

    @Test
    public void testUpdateReport() {
        // given: an audit report config
        AuditReportConfig savedReport = fixture.save(mockAuditReportConfig(UUID.randomUUID())
            .addParameter("key1", "value1")
            .addParameter("key2", "value2")
            .addParameter("key3", "value3"));
        fixture.flush();

        // clear the entity cache
        fixture.getEntityManager().clear();

        // and: the report is retrieved
        AuditReportConfig retrievedReport = fixture.findByIdOptional(savedReport.getId())
            .orElseThrow();

        // and: the report is modified
        retrievedReport.setName(randomAlphanumeric(30));
        retrievedReport.setDescription(randomAlphanumeric(30));
        assertEquals("value2", retrievedReport.getParameters().remove("key2").getValue());
        retrievedReport.getParameters().forEach((key, entry) -> entry.setValue(randomAlphanumeric(30)));
        retrievedReport.addParameter("key4", "value4");
        retrievedReport.addParameter("key5", "value5");

        // when: the modified report is updated
        AuditReportConfig updatedReport = fixture.save(retrievedReport);
        fixture.flush();

        // then: the updated report reflects the changes
        assertEquals(retrievedReport.getName(), updatedReport.getName());
        assertEquals(retrievedReport.getDescription(), updatedReport.getDescription());
        assertEquals(retrievedReport.getParameters().size(), updatedReport.getParameters().size());
        retrievedReport.getParameters().forEach((key, expectedValue) -> {
            AuditReportParameter actual = updatedReport.getParameters().get(key);
            assertNotNull(actual);
            assertEquals(expectedValue.getValue(), actual.getValue());
        });
    }

    @Test
    public void testUpdateReport_AllParameters() {
        // given: an audit report config
        AuditReportConfig savedReport = fixture.save(mockAuditReportConfig(UUID.randomUUID())
            .addParameter("key1", "value1")
            .addParameter("key2", "value2")
            .addParameter("key3", "value3"));
        fixture.flush();

        // clear the entity cache
        fixture.getEntityManager().clear();

        // and: the report is retrieved
        AuditReportConfig retrievedReport = fixture.findByIdOptional(savedReport.getId())
            .orElseThrow();

        // and: a new collection of report parameters are provided
        AuditReportConfig update = mockAuditReportConfig(UUID.randomUUID())
            .addParameter("key1", "newValue1")
            .addParameter("key3", "newValue2")
            .addParameter("newKey1", "newValue1")
            .addParameter("newKey2", "newValue2");


        // and: the existing parameters are updated or added
        update.getParameters().values().forEach(param ->
            retrievedReport.getParameter(param.getName()).ifPresentOrElse(
                p -> p.setValue(param.getValue()),
                () -> retrievedReport.addParameter(param.getName(), param.getValue())
            )
        );

        // and: the delete missing parameters
        retrievedReport.getParameters().keySet().stream()
            .filter(paramName -> !update.getParameters().containsKey(paramName))
            .toList()
            .forEach(retrievedReport::removeParameter);

        // when: the modified report is updated
        AuditReportConfig updatedReport = fixture.save(retrievedReport);
        fixture.flush();

        // then: the updated report reflects the changes
        assertEquals(update.getParameters().size(), updatedReport.getParameters().size());
        update.getParameters().forEach((key, expectedValue) ->
            updatedReport.getParameter(expectedValue.getName())
                .ifPresentOrElse(
                    actual -> assertEquals(expectedValue.getValue(), actual.getValue()),
                    () -> fail("Parameter not found: " + expectedValue.getName())
                )
        );
    }

    @Test
    public void testDeleteReportById() {
        // given: an audit report config
        AuditReportConfig savedReport = fixture.save(mockAuditReportConfig(UUID.randomUUID())
            .addParameter("key1", "value1")
            .addParameter("key2", "value2")
            .addParameter("key3", "value3"));
        fixture.flush();

        // clear the entity cache
        fixture.getEntityManager().clear();

        // when: the report is deleted
        assertTrue(fixture.deleteById(savedReport.getId()));
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the report is no longer in the database
        assertTrue(fixture.findByIdOptional(savedReport.getId()).isEmpty());
    }

    @Test
    public void testFindByUserId() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: a collection of audit report configs for that user
        List<AuditReportConfig> auditReports = IntStream.range(0, 30)
            .mapToObj(i -> fixture.save(mockAuditReportConfig(userId)))
            .sorted(Comparator.comparing(AuditReportConfig::getName, String::compareToIgnoreCase))
            .toList();

        // and: reports exist for other users
        IntStream.range(0, 30).forEach(i ->
            fixture.save(mockAuditReportConfig(UUID.randomUUID()))
        );

        fixture.flush();
        fixture.getEntityManager().clear();

        // when: the reports are retrieved by user identity by page
        int pageSize = 10;
        int pageCount = 3;
        for (int page = 0; page < pageCount; page++) {
            List<AuditReportConfig> reportsPage = fixture.findByUserId(userId, page, pageSize).getContent();

            // then: the reports are retrieved for that page
            assertEquals(pageSize, reportsPage.size());
            assertEquals(auditReports.subList(page * pageSize, (page + 1) * pageSize), reportsPage);
        }
    }

    @Test
    public void testListUserIds() {
        // given: a collection of audit report configs for different users
        List<UUID> userIds = IntStream.range(0, 30)
            .mapToObj(i -> UUID.randomUUID())
            .toList();
        userIds.forEach(userId ->
            IntStream.range(0, 3)
                .forEach(i -> fixture.save(mockAuditReportConfig(userId)))
        );

        // when: the user identities are retrieved
        List<UUID> retrievedUserIds = fixture.listUserIds();

        // then: the user identities are retrieved
        assertEquals(userIds.size(), retrievedUserIds.size());
        assertTrue(userIds.containsAll(retrievedUserIds));
    }

    @Test
    public void testDeleteByReportSource() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: a report source identity
        UUID reportSourceId = UUID.randomUUID();

        // and: a collection of reports linked to specific report sources
        List<AuditReportConfig> auditReports = IntStream.range(0, 3)
            .mapToObj(i -> mockAuditReportConfig(userId, b -> b.reportSourceId(reportSourceId)))
            .toList();
        fixture.saveAll(auditReports);

        // and: a collection of reports linked to difference report sources
        List<AuditReportConfig> otherReports = IntStream.range(0, 5)
            .mapToObj(i -> mockAuditReportConfig(userId))
            .toList();
        fixture.saveAll(otherReports);

        fixture.flush();
        fixture.getEntityManager().clear();

        // when: report source is deleted
        fixture.deleteByReportSource(reportSourceId);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the report source entries cannot be found
        auditReports.forEach(report ->
            assertTrue(fixture.findByIdOptional(report.getId()).isEmpty())
        );

        // and: only the other reports can be found
        otherReports.forEach(report ->
            assertTrue(fixture.findByIdOptional(report.getId()).isPresent())
        );
    }
}
