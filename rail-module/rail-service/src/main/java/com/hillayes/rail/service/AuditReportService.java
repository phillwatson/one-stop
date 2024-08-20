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
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.AuditIssueRepository;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class AuditReportService {
    @Inject
    @Any
    Instance<AuditReportTemplate> reportTemplates;

    @Inject
    AuditReportConfigRepository auditReportConfigRepository;

    @Inject
    AuditIssueRepository auditIssueRepository;

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    public Page<AuditReportTemplate> getAuditTemplates(int page, int pageSize) {
        log.info("Listing audit report templates [page: {}, pageSize: {}]", page, pageSize);
        List<AuditReportTemplate> templates = reportTemplates.stream()
            .sorted(Comparator.comparing(AuditReportTemplate::getName, String::compareToIgnoreCase))
            .toList();
        return Page.of(templates, page, pageSize);
    }

    public Page<AuditReportConfig> getAuditConfigs(UUID userId, int page, int pageSize) {
        log.info("Listing audit report configs [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        return auditReportConfigRepository.findByUserId(userId, page, pageSize);
    }

    public AuditReportConfig createAuditConfig(UUID userId, AuditReportConfig config) {
        log.info("Creating audit report config [userId: {}, name: {}]", userId, config.getName());
        String newName = Strings.trimOrNull(config.getName());
        if (newName == null) {
            throw new MissingParameterException("AuditReportConfig.name");
        }

        auditReportConfigRepository.findByUserAndName(userId, newName)
            .ifPresent(existing -> { throw new AuditReportConfigAlreadyExistsException(existing); });

        config.setUserId(userId);
        config.setName(newName);
        return auditReportConfigRepository.save(config);
    }

    public AuditReportConfig getAuditConfig(UUID userId, UUID configId) {
        log.info("Getting audit report config [userId: {}, configId: {}]", userId, configId);
        return auditReportConfigRepository.findByIdOptional(configId)
            .filter(config -> config.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("AuditReportConfig", configId));
    }

    public AuditReportConfig updateAuditConfig(UUID userId, UUID configId, AuditReportConfig update) {
        log.info("Updating audit report config [userId: {}, configId: {}, name: {}]", userId, configId, update.getName());

        AuditReportConfig original = getAuditConfig(userId, configId);

        String newName = Strings.trimOrNull(update.getName());
        if (newName == null) {
            throw new MissingParameterException("AuditReportConfig.name");
        }

        auditReportConfigRepository.findByUserAndName(userId, newName)
            .filter(existing -> !existing.getId().equals(configId))
            .ifPresent(existing -> { throw new AuditReportConfigAlreadyExistsException(existing); });

        original.setVersion(update.getVersion());
        original.setDisabled(update.isDisabled());
        original.setName(update.getName());
        original.setDescription(update.getDescription());
        original.setTemplateName(update.getTemplateName());
        original.setReportSource(update.getReportSource());
        original.setReportSourceId(update.getReportSourceId());
        original.setUncategorisedIncluded(update.isUncategorisedIncluded());

        // update existing or add new parameters
        update.getParameters().values().forEach(param ->
            original.getParameter(param.getName()).ifPresentOrElse(
                p -> p.setValue(param.getValue()),
                () -> original.addParameter(param.getName(), param.getValue())
            )
        );

        // delete missing parameters
        original.getParameters().keySet().stream()
            .filter(paramName -> !update.getParameters().containsKey(paramName))
            .toList()
            .forEach(original::removeParameter);

        return auditReportConfigRepository.save(original);
    }

    public void deleteAuditConfig(UUID userId, UUID configId) {
        log.info("Deleting audit report config [userId: {}, configId: {}]", userId, configId);
        auditReportConfigRepository.delete(getAuditConfig(userId, configId));
    }

    public void deleteAllAuditConfigs(UUID userId) {
        log.info("Deleting audit report configs [userId: {}]", userId);
        auditReportConfigRepository.deleteByUserId(userId);
    }

    public List<AuditIssueSummary> getIssueSummaries(UUID userId) {
        log.info("Get audit issue summaries [userId: {}]", userId);
        return auditIssueRepository.getIssueSummaries(userId);
    }

    public Page<AuditIssue> getAuditIssues(UUID userId, UUID configId, Boolean acknowledged,
                                           int page, int pageSize) {
        log.info("Listing audit report issues [userId: {}, configId: {}, acknowledged: {}, page: {}, pageSize: {}]",
            userId, configId, acknowledged, page, pageSize);
        getAuditConfig(userId, configId);
        return auditIssueRepository.findByConfigId(configId, acknowledged, page, pageSize);
    }

    public AuditIssue getAuditIssue(UUID userId, UUID issueId) {
        log.info("Get audit report issue [userId: {}, issueId: {}]", userId, issueId);
        return auditIssueRepository.findByIdOptional(issueId)
            .filter(issue -> issue.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("AuditIssue", issueId));
    }

    public AuditIssue updateIssue(UUID userId, UUID issueId, boolean acknowledged) {
        log.info("Updating audit issue [issueId: {}, acknowledged: {}]", issueId, acknowledged);
        AuditIssue issue = auditIssueRepository.findByIdOptional(issueId)
            .filter(i -> i.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("AuditIssue", issueId));
        issue.setAcknowledgedDateTime(acknowledged ? Instant.now() : null);
        return auditIssueRepository.save(issue);
    }

    public void deleteIssue(UUID userId, UUID issueId) {
        log.info("Deleting audit issue [issueId: {}]", issueId);
        auditIssueRepository.delete(auditIssueRepository.findByIdOptional(issueId)
            .filter(i -> i.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("AuditIssue", issueId)));
    }
}
