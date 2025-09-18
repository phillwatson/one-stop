package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.Page;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.event.AuditEventSender;
import com.hillayes.rail.repository.AuditIssueRepository;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.*;

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
    AuditIssueRepository auditIssueRepository;

    @Inject
    AuditEventSender auditEventSender;

    @ConfigProperty(name = "one-stop.rail.audit.issues.ack-timeout")
    Optional<Duration> ackTimeout;

    @RegisterForReflection
    public record Payload(
        UUID userId
    ) { }

    public String getName() {
        return "user-audit-reports";
    }

    public String queueJob(UUID userId) {
        log.info("Queuing job [userId: {}]", userId);
        return queueTask(new Payload(userId));
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

        Map<AuditReportConfig, List<AuditIssue>> allIssues = new HashMap<>();
        int page = 0;
        Page<AuditReportConfig> reportConfigs;
        do {
            reportConfigs = auditReportConfigRepository.findByUserId(userId, page, 10);
            reportConfigs.forEach(config -> {
                // delete any old acknowledged issues
                ackTimeout.ifPresent(timeout ->
                    auditIssueRepository.deleteAcknowledged(config.getId(), timeout)
                );

                if (!config.isDisabled()) {
                    List<AuditIssue> auditIssues = runReport(config);
                    if (!auditIssues.isEmpty()) {
                        allIssues.put(config, auditIssues);
                    }
                }
            });

            page++;
        } while (page < reportConfigs.getTotalPages());

        // if any issues were found
        if (!allIssues.isEmpty()) {
            Map<String, Integer> reportCounts = new HashMap<>();
            allIssues.forEach((config, issues) -> {
                // collate the issue count for each report
                reportCounts.put(config.getName(), issues.size());

                // save them in batches
                auditIssueRepository.saveAll(issues);
            });

            // send an event summarising the issues found within each report
            auditEventSender.sendAuditIssuesFound(userId, reportCounts);
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
