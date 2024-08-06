package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.Page;
import com.hillayes.events.events.audit.AuditIssue;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.domain.AuditReportParameter;
import com.hillayes.rail.event.AuditEventSender;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A jobbing task to run the audit reports configured by the identified user.
 */
@ApplicationScoped
@Slf4j
public class UserAuditReportsJobbingTask extends AbstractNamedJobbingTask<UserAuditReportsJobbingTask.Payload> {
    @Inject
    AuditReportConfigRepository auditReportConfigRepository;

    @Inject
    @Any
    Instance<AuditReportTemplate> reportTemplates;

    @Inject
    AuditEventSender auditEventSender;

    @RegisterForReflection
    public record Payload(
        UUID userId
    ) {}

    public String getName() {
        return "user-audit-reports";
    }

    public String queueJob(UUID userId) {
        log.info("Queuing job [userId: {}]", userId);
        Payload payload = new Payload(userId);
        return scheduler.addJob(this, payload);
    }

    /**
     * Performs the task of running the audit reports configured by the identified user.
     *
     * @param context the context containing the identifier of the User whose reports are to be run.
     */
    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<Payload> context) {
        UUID userId = context.getPayload().userId();
        log.info("Processing User Audit Reports job [userId: {}]", userId);

        List<AuditIssue> issues = new ArrayList<>();
        int page = 0;
        Page<AuditReportConfig> reportConfigs;
        do {
            reportConfigs = auditReportConfigRepository.findByUserId(userId, page, 10);
            issues.addAll(reportConfigs.stream()
                .filter(config -> !config.isDisabled())
                .map(this::runReport)
                .toList()
            );

            page++;
        } while (page < reportConfigs.getTotalPages());

        if (!issues.isEmpty()) {
            auditEventSender.sendAuditIssuesFound(userId, issues);
        }
        return TaskConclusion.COMPLETE;
    }

    private AuditIssue runReport(AuditReportConfig config) {
        return reportTemplates.stream()
            .filter(t -> t.getId().equals(config.getTemplateId()))
            .findFirst()
            .map(template -> template.run(config))
            .map(issues -> marshal(config, issues))
            .orElse(null);
    }

    private AuditIssue marshal(AuditReportConfig config, List<String> issues) {
        return AuditIssue.builder()
            .auditReportId(config.getTemplateId())
            .reportName(config.getName())
            .reportDescription(config.getDescription())
            .reportParameters(config.getParameters().values().stream()
                .collect(Collectors.toMap(AuditReportParameter::getName, AuditReportParameter::getValue)))
            .issues(issues)
            .build();
    }
}
