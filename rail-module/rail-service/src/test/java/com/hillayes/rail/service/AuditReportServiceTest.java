package com.hillayes.rail.service;

import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.errors.AuditReportConfigAlreadyExistsException;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.rail.utils.TestData.mockAuditReportConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class AuditReportServiceTest {
    List<AuditReportTemplate> mockTemplates;

    Instance<AuditReportTemplate> reportTemplates;

    @Mock
    AuditReportConfigRepository auditReportConfigRepository;

    @InjectMocks
    AuditReportService fixture;

    @BeforeEach
    public void init() {
        mockTemplates = IntStream.range(0, 20)
            .mapToObj(i -> mockAuditReportTemplate(randomAlphanumeric(10)))
            .toList();

        reportTemplates = mock(Instance.class);
        when(reportTemplates.stream()).thenReturn(mockTemplates.stream());

        openMocks(this);
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

        // and: the repository returns the new config
        when(auditReportConfigRepository.save(config)).thenReturn(config);

        // when: the new config is created
        AuditReportConfig created = fixture.createAuditConfig(userId, config);

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).save(config);

        // and: the new config is returned
        assertEquals(config, created);

        // and: the config name is trimmed of spaces
        assertEquals("name with leading and trailing spaces", created.getName());
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

        // and: the repository returns the new config
        when(auditReportConfigRepository.save(config)).thenReturn(config);

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

        // and: an updated report config
        AuditReportConfig update = mockAuditReportConfig(userId, b -> b.id(original.getId()));

        // and: the repository returns the existing config
        when(auditReportConfigRepository.findByIdOptional(original.getId()))
            .thenReturn(Optional.of(original));

        // and: no other existing config exists with the same name
        when(auditReportConfigRepository.findByUserAndName(userId, update.getName()))
            .thenReturn(Optional.empty());

        // and: the repository returns the updated config
        when(auditReportConfigRepository.save(original)).thenReturn(update);

        // when: the existing config is updated
        AuditReportConfig updated = fixture.updateAuditConfig(userId, original.getId(), update);

        // then: the repository is invoked with the given parameters
        verify(auditReportConfigRepository).findByIdOptional(original.getId());
        verify(auditReportConfigRepository).save(original);

        // and: the updated config is returned
        assertEquals(update, updated);
    }

    @Test
    public void testUpdateAuditConfig_DuplicateName() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an existing report config
        AuditReportConfig original = mockAuditReportConfig(userId, b -> b.id(UUID.randomUUID()));

        // and: an updated report config
        AuditReportConfig update = mockAuditReportConfig(userId, b -> b.id(original.getId()));

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

        // and: the repository returns the updated config
        when(auditReportConfigRepository.save(original)).thenReturn(update);

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
