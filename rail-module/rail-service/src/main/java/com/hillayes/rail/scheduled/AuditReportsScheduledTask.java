package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * A scheduled task to run all the configured audit reports. To share the load,
 * this task queues a UserAuditReportsJobbingTask for each user that has at least
 * one configured audit report.
 */
@ApplicationScoped
@Slf4j
public class AuditReportsScheduledTask implements NamedScheduledTask {
    @Inject
    AuditReportConfigRepository auditReportRepository;

    @Inject
    UserAuditReportsJobbingTask userAuditReportsJobbingTask;


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
     * audit report, and queues a job to run the reports for those users.
     */
    @Override
    @Transactional
    public void run() {
        log.info("AuditReportsScheduledTask.run()");

        // queue a job to run the reports for each user
        auditReportRepository.listUserIds()
            .forEach(userId -> userAuditReportsJobbingTask.queueJob(userId));
    }
}
