package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A scheduled task to run all the configured audit reports. To share the load,
 * this task queues a UserAuditReportsAdhocTask for each user that has at least
 * one configured audit report.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AuditReportsScheduledTask implements NamedScheduledTask {
    private final AuditReportConfigRepository auditReportRepository;
    private final UserAuditReportsAdhocTask userAuditReportsAdhocTask;


    @Override
    public String getName() {
        return "audit-reports";
    }

    @Override
    public void taskInitialised(SchedulerFactory scheduler) {
        log.info("AuditReportsScheduledTask.taskScheduled()");
    }

    /**
     * Retrieves the list of identifiers of users that have at least one configured
     * audit report, and queues a task to run the reports for those users.
     */
    @Override
    @Transactional
    public void run() {
        log.info("AuditReportsScheduledTask.run()");

        // queue a task to run the reports for each user
        auditReportRepository.listUserIds()
            .forEach(userId -> userAuditReportsAdhocTask.queueTask(userId));
    }
}
