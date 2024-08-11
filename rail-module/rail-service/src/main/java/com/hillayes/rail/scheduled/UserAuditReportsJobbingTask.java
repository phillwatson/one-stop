package com.hillayes.rail.scheduled;

import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.event.AuditEventSender;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    ) { }

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

        Map<String, Integer> issueCounts = new HashMap<>();
        int page = 0;
        Page<AuditReportConfig> reportConfigs;
        do {
            reportConfigs = auditReportConfigRepository.findByUserId(userId, page, 10);
            reportConfigs.forEach(config -> {
                if (!config.isDisabled()) {
                    List<AuditIssue> auditIssues = runReport(config);
                    if (!auditIssues.isEmpty()) {
                        issueCounts.put(
                            Strings.getOrDefault(config.getDescription(), config.getName()),
                            auditIssues.size()
                        );
                    }
                }
            });

            page++;
        } while (page < reportConfigs.getTotalPages());

        if (!issueCounts.isEmpty()) {
            auditEventSender.sendAuditIssuesFound(userId, issueCounts);
        }
        return TaskConclusion.COMPLETE;
    }

    private List<AuditIssue> runReport(AuditReportConfig config) {
        return reportTemplates.stream()
            .filter(t -> t.getName().equals(config.getTemplateName()))
            .findFirst()
            .map(template -> template.run(config))
            .orElse(List.of());
    }
}
