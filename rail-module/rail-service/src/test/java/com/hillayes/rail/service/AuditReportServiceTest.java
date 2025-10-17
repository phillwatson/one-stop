package com.hillayes.rail.service;

import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditIssueSummary;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.errors.AuditReportConfigAlreadyExistsException;
import com.hillayes.rail.repository.AuditIssueRepository;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import com.hillayes.rail.utils.TestData;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.rail.utils.TestData.mockAuditIssue;
import static com.hillayes.rail.utils.TestData.mockAuditReportConfig;
import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuditReportServiceTest {
    private final List<AuditReportTemplate> mockTemplates = IntStream.range(0, 20)
        .mapToObj(i -> mockAuditReportTemplate(insecure().nextAlphanumeric(10)))
        .toList();

    private final Instance<AuditReportTemplate> reportTemplates = mock();
    private final AuditReportConfigRepository auditReportConfigRepository = mock();
    private final AuditIssueRepository auditIssueRepository = mock();

    private final AuditReportService fixture = new AuditReportService(
        reportTemplates,
        auditReportConfigRepository,
        auditIssueRepository
    );

    @BeforeEach
    public void init() {
        when(reportTemplates.stream()).thenReturn(mockTemplates.stream());

        when(auditIssueRepository.save(any())).thenAnswer(i -> {
            AuditIssue issue = i.getArgument(0);
            if (issue.getId() == null) {
                issue.setId(UUID.randomUUID());
            }
            return issue;
        });

        when(auditReportConfigRepository.save(any())).thenAnswer(i -> {
            AuditReportConfig config = i.getArgument(0);
            if (config.getId() == null) {
                config.setId(UUID.randomUUID());
            }
            return config;
        });
    }

    @Test
    public void testGetAuditConfigs() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an expected page of report configs
        List<AuditReportConfig> expected = IntStream.range(0, 10)
            .mapToObj(i -> mockAuditReportConfig(userId))
            .toList();

        // and: the repository returns the expected page of configs
        when(auditReportConfigRepository.findByUserId(userId, 0, 10))
            .thenReturn(Page.of(expected, 0, 10));

        // when: a page of configs is requested
        Page<AuditReportConfig> configs = fixture.getAuditConfigs(userId, 0, 10);

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByUserId(userId, 0, 10);

        // and: the expected page of configs is returned
        assertEquals(expected.size(), configs.getContent().size());
        assertEquals(expected, configs.getContent());
    }

    @Test
    public void testCreateAuditConfig() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: a new report config
        AuditReportConfig config = mockAuditReportConfig(userId, b ->
            b.name("  name with leading and trailing spaces   "));

        // and: no existing config with the same name (trimmed)
        when(auditReportConfigRepository.findByUserAndName(userId, Strings.trimOrNull(config.getName())))
            .thenReturn(Optional.empty());

        // when: the new config is created
        AuditReportConfig created = fixture.createAuditConfig(userId, config);

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).save(config);

        // and: the new config is returned
        assertEquals(config, created);

        // and: the config name is trimmed of spaces
        assertEquals("name with leading and trailing spaces", created.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  " })
    public void testCreateAuditConfig_NoName(String nameValue) {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: a new config with no name
        AuditReportConfig config = mockAuditReportConfig(userId, c -> c.name(nameValue));

        // when: the new config is created
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.createAuditConfig(userId, config));

        // then: the repository is NOT invoked to save the new config
        verify(auditReportConfigRepository, never()).save(any());

        // and: the exception identifies the missing value
        assertEquals("AuditReportConfig.name", exception.getParameter("parameter-name"));
    }

    @Test
    public void testCreateAuditConfig_DuplicateName() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: a new report config
        AuditReportConfig config = mockAuditReportConfig(userId);

        // and: an existing config with the same name
        AuditReportConfig existing = mockAuditReportConfig(userId, c -> c.id(UUID.randomUUID()));
        when(auditReportConfigRepository.findByUserAndName(userId, config.getName()))
            .thenReturn(Optional.of(existing));

        // when: the new config is created
        AuditReportConfigAlreadyExistsException exception = assertThrows(AuditReportConfigAlreadyExistsException.class, () ->
            fixture.createAuditConfig(userId, config));

        // then: the repository is NOT invoked to save the new config
        verify(auditReportConfigRepository, never()).save(any());

        // and: the exception identifies the existing config
        assertEquals(existing.getId(), exception.getParameter("id"));
        assertEquals(existing.getName(), exception.getParameter("name"));
    }

    @Test
    public void testGetAuditConfig() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig config = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // when: the existing config is requested
        AuditReportConfig retrieved = fixture.getAuditConfig(userId, config.getId());

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(config.getId());

        // and: the existing config is returned
        assertEquals(config, retrieved);
    }

    @Test
    public void testGetAuditConfig_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config - belonging to another user
        AuditReportConfig config = mockAuditReportConfig(UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // when: the existing config is requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.getAuditConfig(userId, config.getId()));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(config.getId());

        // and: an exception identifies the error
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(config.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testUpdateAuditConfig() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig original = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));
        original.addParameter("param1", "value1");
        original.addParameter("param2", "value2");

        // and: an updated report config
        AuditReportConfig update = mockAuditReportConfig(userId, b -> b.id(original.getId()));
        update.addParameter("param1", "value1-updated");
        update.addParameter("param3", "value3");

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(original.getId()))
            .thenReturn(Optional.of(original));

        // and: no other existing config exists with the same name
        when(auditReportConfigRepository.findByUserAndName(userId, update.getName()))
            .thenReturn(Optional.empty());

        // when: the existing config is updated
        AuditReportConfig updated = fixture.updateAuditConfig(userId, original.getId(), update);

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(original.getId());
        verify(auditReportConfigRepository).save(original);

        // and: the updated config is returned
        assertEquals(update, updated);

        // and: the parameters are as updated where provided
        assertEquals(2, updated.getParameters().size());
        assertEquals("value1-updated", updated.getParameter("param1").get().getValue());
        assertEquals("value3", updated.getParameter("param3").get().getValue());

        // and: the parameters are removed where not provided
        assertTrue(updated.getParameter("param2").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  " })
    public void testUpdateAuditConfig_NoName(String nameValue) {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig original = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: an updated report config - with no name
        AuditReportConfig update = mockAuditReportConfig(userId, b -> b.name(nameValue));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(original.getId()))
            .thenReturn(Optional.of(original));

        // when: the existing config is updated
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateAuditConfig(userId, original.getId(), update));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(original.getId());

        // and: no update is saved
        verify(auditReportConfigRepository, never()).save(any());

        // and: the exception identifies the missing value
        assertEquals("AuditReportConfig.name", exception.getParameter("parameter-name"));
    }

    @Test
    public void testUpdateAuditConfig_DuplicateName() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig original = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: an updated report config
        AuditReportConfig update = mockAuditReportConfig(userId);

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(original.getId()))
            .thenReturn(Optional.of(original));

        // and: no other existing config exists with the same name as update
        AuditReportConfig existing = mockAuditReportConfig(userId, b -> {
            b.id(UUID.randomUUID());
            b.name(update.getName());
        });
        when(auditReportConfigRepository.findByUserAndName(userId, update.getName()))
            .thenReturn(Optional.of(existing));

        // when: the existing config is updated
        AuditReportConfigAlreadyExistsException exception = assertThrows(AuditReportConfigAlreadyExistsException.class, () ->
            fixture.updateAuditConfig(userId, original.getId(), update));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(original.getId());

        // and: no update is saved
        verify(auditReportConfigRepository, never()).save(any());

        // and: the exception identifies the existing config
        assertEquals(existing.getId(), exception.getParameter("id"));
        assertEquals(existing.getName(), exception.getParameter("name"));
    }

    @Test
    public void testUpdateAuditConfig_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an updated report config
        AuditReportConfig update = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: an existing report config - belonging to another user
        AuditReportConfig config = mockAuditReportConfig(UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // when: the existing config is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.updateAuditConfig(userId, update.getId(), update));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(update.getId());

        // and: no update is saved
        verify(auditReportConfigRepository, never()).save(any());

        // and: the exception identifies the existing config
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(update.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testUpdateAuditConfig_NotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an updated report config
        AuditReportConfig update = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: the repository returns NO existing config
        when(auditReportConfigRepository.findByIdOptional(update.getId()))
            .thenReturn(Optional.empty());

        // when: the existing config is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.updateAuditConfig(userId, update.getId(), update));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(update.getId());

        // and: no update is saved
        verify(auditReportConfigRepository, never()).save(any());

        // and: the exception identifies the existing config
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(update.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteAuditConfig() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig config = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // when: the existing config is deleted
        fixture.deleteAuditConfig(userId, config.getId());

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).delete(config);
    }

    @Test
    public void testDeleteAuditConfig_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config - belonging to another user
        AuditReportConfig config = mockAuditReportConfig(UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // when: the existing config is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.deleteAuditConfig(userId, config.getId()));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(config.getId());

        // and: no update is deleted
        verify(auditReportConfigRepository, never()).delete(any());

        // and: the exception identifies the existing config
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(config.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteAuditConfig_NotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        UUID configId = UUID.randomUUID();

        // and: NO existing config
        when(auditReportConfigRepository.findByIdOptional(configId))
            .thenReturn(Optional.empty());

        // when: the existing config is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.deleteAuditConfig(userId, configId));

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(configId);

        // and: no update is deleted
        verify(auditReportConfigRepository, never()).delete(any());

        // and: the exception identifies the existing config
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(configId, exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteAllAuditConfigs() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // when: all configs are deleted
        fixture.deleteAllAuditConfigs(userId);

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).deleteByUserId(userId);
    }

    @Test
    public void testGetIssueSummaries() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: existing report configs
        List<AuditReportConfig> configs = IntStream.range(0, 5)
            .mapToObj(i -> mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID())))
            .toList();

        // and: issues summaries for each report
        List<AuditIssueSummary> issueSummaries = configs.stream()
            .map(TestData::mockAuditIssueSummary)
            .toList();
        when(auditIssueRepository.getIssueSummaries(userId))
            .thenReturn(issueSummaries);

        // when: the service is called
        List<AuditIssueSummary> result = fixture.getIssueSummaries(userId);

        // then: the repository is asked to get the summaries
        verify(auditIssueRepository).getIssueSummaries(userId);

        // and: the summaries are returned
        assertEquals(issueSummaries.size(), result.size());
        issueSummaries.forEach(expected ->
            assertTrue(result.stream().anyMatch(actual ->
                expected.getAuditConfigId().equals(actual.getAuditConfigId()))
            )
        );
    }

    @Test
    public void testGetAuditIssues() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig config = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // and: a collection issues exist
        List<AuditIssue> allIssues = IntStream.range(0, 35)
            .mapToObj(i -> mockAuditIssue(config, issue -> issue.id(UUID.randomUUID())))
            .toList();
        when(auditIssueRepository.findByConfigId(eq(config.getId()), nullable(Boolean.class), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(2);
                int pageSize = invocation.getArgument(3);
                return Page.of(allIssues, pageIndex, pageSize);
            });

        // when: each page of issues is requested
        int pageIndex = 0;
        do {
            Page<AuditIssue> issuesPage = fixture.getAuditIssues(userId, config.getId(), null, pageIndex, 10);

            // then: the config identity and ownership is verified
            verify(auditReportConfigRepository, times(pageIndex + 1)).findByIdOptional(config.getId());

            // and: the issues repository is invoked with the given parameters
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            verify(auditIssueRepository, times(pageIndex + 1)).findByConfigId(eq(config.getId()), isNull(), captor.capture(), eq(10));
            assertEquals(pageIndex, captor.getValue());

            // and: the issues are returned
            assertEquals((pageIndex < 3) ? 10 : 5, issuesPage.getContent().size());
        } while (++pageIndex < 4);
    }

    @Test
    public void testGetAuditIssues_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config - belonging to another user
        AuditReportConfig config = mockAuditReportConfig(UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(config.getId()))
            .thenReturn(Optional.of(config));

        // when: the issues are requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.getAuditIssues(userId, config.getId(), null, 0, 10));

        // then: the config identity and ownership is verified
        verify(auditReportConfigRepository).findByIdOptional(config.getId());

        // and: the issues repository is NOT invoked
        verify(auditIssueRepository, never()).findByConfigId(any(), any(), anyInt(), anyInt());

        // and: an exception identifies the existing config
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(config.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testGetAuditIssues_NotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        UUID configId = UUID.randomUUID();

        // and: NO existing config
        when(auditReportConfigRepository.findByIdOptional(configId))
            .thenReturn(Optional.empty());

        // when: the issues are requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.getAuditIssues(userId, configId, null, 0, 10));

        // then: the config identity and ownership is verified
        verify(auditReportConfigRepository).findByIdOptional(configId);

        // and: the issues repository is NOT invoked
        verify(auditIssueRepository, never()).findByConfigId(any(), any(), anyInt(), anyInt());

        // and: an exception identifies the existing config
        assertEquals("AuditReportConfig", exception.getParameter("entity-type"));
        assertEquals(configId, exception.getParameter("entity-id"));
    }

    @Test
    public void testGetAuditIssue() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue
        AuditIssue issue = mockAuditIssue(userId, UUID.randomUUID(), b -> b.id(UUID.randomUUID()));
        when(auditIssueRepository.findByIdOptional(issue.getId()))
            .thenReturn(Optional.of(issue));

        // when: the existing issue is requested
        AuditIssue retrieved = fixture.getAuditIssue(userId, issue.getId());

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issue.getId());

        // and: the existing issue is returned
        assertEquals(issue, retrieved);
    }

    @Test
    public void testGetAuditIssue_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue - belonging to another user
        AuditIssue issue = mockAuditIssue(UUID.randomUUID(), UUID.randomUUID(), b -> b.id(UUID.randomUUID()));
        when(auditIssueRepository.findByIdOptional(issue.getId()))
            .thenReturn(Optional.of(issue));

        // when: the existing issue is requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.getAuditIssue(userId, issue.getId()));

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issue.getId());

        // and: an exception identifies the existing issue
        assertEquals("AuditIssue", exception.getParameter("entity-type"));
        assertEquals(issue.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testGetAuditIssue_NotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue
        UUID issueId = UUID.randomUUID();

        // and: NO existing issue
        when(auditIssueRepository.findByIdOptional(issueId))
            .thenReturn(Optional.empty());

        // when: the existing issue is requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.getAuditIssue(userId, issueId));

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issueId);

        // and: an exception identifies the existing issue
        assertEquals("AuditIssue", exception.getParameter("entity-type"));
        assertEquals(issueId, exception.getParameter("entity-id"));
    }

    @Test
    public void testUpdateIssue() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue
        AuditIssue issue = mockAuditIssue(userId, UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing issue
        when(auditIssueRepository.findByIdOptional(issue.getId()))
            .thenReturn(Optional.of(issue));

        // when: the existing issue is updated
        AuditIssue updated = fixture.updateIssue(userId, issue.getId(), true);

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issue.getId());
        verify(auditIssueRepository).save(issue);

        // and: the updated issue is returned
        assertEquals(issue, updated);
        assertTrue(updated.isAcknowledged());
    }

    @Test
    public void testUpdateIssue_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue - belonging to another user
        AuditIssue issue = mockAuditIssue(UUID.randomUUID(), UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing issue
        when(auditIssueRepository.findByIdOptional(issue.getId()))
            .thenReturn(Optional.of(issue));

        // when: the existing issue is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.updateIssue(userId, issue.getId(), true));

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issue.getId());

        // and: no update is made
        verify(auditIssueRepository, never()).save(any());

        // and: an exception identifies the existing issue
        assertEquals("AuditIssue", exception.getParameter("entity-type"));
        assertEquals(issue.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testUpdateIssue_NotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue
        UUID issueId = UUID.randomUUID();

        // and: NO existing issue
        when(auditIssueRepository.findByIdOptional(issueId))
            .thenReturn(Optional.empty());

        // when: the existing issue is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.updateIssue(userId, issueId, true));

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issueId);

        // and: no update is made
        verify(auditIssueRepository, never()).save(any());

        // and: an exception identifies the existing issue
        assertEquals("AuditIssue", exception.getParameter("entity-type"));
        assertEquals(issueId, exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteIssue() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue
        AuditIssue issue = mockAuditIssue(userId, UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing issue
        when(auditIssueRepository.findByIdOptional(issue.getId()))
            .thenReturn(Optional.of(issue));

        // when: the existing issue is deleted
        fixture.deleteIssue(userId, issue.getId());

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).delete(issue);
    }

    @Test
    public void testDeleteIssue_WrongUser() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue - belonging to another user
        AuditIssue issue = mockAuditIssue(UUID.randomUUID(), UUID.randomUUID(), b -> b.id(UUID.randomUUID()));

        // and: the repository returns the existing issue
        when(auditIssueRepository.findByIdOptional(issue.getId()))
            .thenReturn(Optional.of(issue));

        // when: the existing issue is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.deleteIssue(userId, issue.getId()));

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issue.getId());

        // and: no update is deleted
        verify(auditIssueRepository, never()).delete(any());

        // and: an exception identifies the existing issue
        assertEquals("AuditIssue", exception.getParameter("entity-type"));
        assertEquals(issue.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteIssue_NotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report issue
        UUID issueId = UUID.randomUUID();

        // and: NO existing issue
        when(auditIssueRepository.findByIdOptional(issueId))
            .thenReturn(Optional.empty());

        // when: the existing issue is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.deleteIssue(userId, issueId));

        // then: the repository is invoked with the given parameters
        verify(auditIssueRepository).findByIdOptional(issueId);

        // and: no update is deleted
        verify(auditIssueRepository, never()).delete(any());

        // and: an exception identifies the existing issue
        assertEquals("AuditIssue", exception.getParameter("entity-type"));
        assertEquals(issueId, exception.getParameter("entity-id"));
    }

    @Test
    public void testGetAuditTemplates() {
        // given: an expected page of report templates
        List<AuditReportTemplate> expected = mockTemplates.stream()
            .sorted(Comparator.comparing(AuditReportTemplate::getName, String::compareToIgnoreCase))
            .limit(10)
            .toList();

        // when: a page of templates is requested
        Page<AuditReportTemplate> templates = fixture.getAuditTemplates(0, 10);

        // then: the expected page of templates is returned
        assertEquals(10, templates.getContent().size());

        // and: the templates are sorted by name
        assertEquals(expected, templates.getContent());
    }

    private AuditReportTemplate mockAuditReportTemplate(String name) {
        return new AuditReportTemplate() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public List<AuditIssue> run(AuditReportConfig reportConfig) {
                return List.of();
            }

            @Override
            public List<Parameter> getParameters() {
                return List.of();
            }
        };
    }
}
