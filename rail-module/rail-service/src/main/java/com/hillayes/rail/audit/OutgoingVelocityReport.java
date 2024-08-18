package com.hillayes.rail.audit;

import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.repository.AuditIssueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * An audit report template that identifies outgoing transactions that exceed the average
 * velocity by a certain threshold.
 * <p>
 * Two averages are calculated:
 * <ol>
 * <li>The minor average velocity = the number of outgoing transactions over a short period of time</li>
 * <li>The major average velocity = the number of outgoing transactions over a longer period of time</li>
 * </ol>
 * A threshold velocity is calculated by multiplying the major average velocity by a factor.
 * If the minor average velocity exceeds the threshold, an issue wil be raised for each
 * outgoing transaction within the minor average period.
 */
@ApplicationScoped
@Slf4j
public class OutgoingVelocityReport extends AuditReportTemplate {
    // the number of days over which the overall activity is averaged
    private static final String PARAM_MAJOR_AVERAGE_DAYS = "majorAverageDays";
    private static final Long PARAM_MAJOR_AVERAGE_DAYS_DEFAULT = 30L;

    // the number of days over which the recent activity is averaged
    private static final String PARAM_MINOR_AVERAGE_DAYS = "minorAverageDays";
    private static final Long PARAM_MINOR_AVERAGE_DAYS_DEFAULT = 5L;

    // the factor by which the average velocity is multiplied to calculate the
    // threshold for outgoing transactions to be considered an issue
    private static final String PARAM_VELOCITY_FACTOR = "velocityFactor";
    private static final Double PARAM_VELOCITY_FACTOR_DEFAULT = 1.5;

    // a list of parameters that the user can set when running this report
    private static final List<Parameter> PARAMETERS = List.of(
        new Parameter(PARAM_MAJOR_AVERAGE_DAYS, "The number of days over which the overall velocity is averaged",
            ParameterType.LONG, PARAM_MAJOR_AVERAGE_DAYS_DEFAULT.toString()),
        new Parameter(PARAM_MINOR_AVERAGE_DAYS, "The number of days over which the recent velocity is averaged",
            ParameterType.LONG, PARAM_MINOR_AVERAGE_DAYS_DEFAULT.toString()),
        new Parameter(PARAM_VELOCITY_FACTOR,
            "The factor by which the major average velocity is multiplied to calculate the threshold" +
                " at which the minor average velocity is considered an issue",
            ParameterType.DOUBLE, PARAM_VELOCITY_FACTOR_DEFAULT.toString())
    );

    @Inject
    AuditIssueRepository auditIssueRepository;

    @Override
    public String getName() {
        return "outgoing-velocity-limits";
    }

    @Override
    public String getDescription() {
        return "Detects outgoing transactions whose average velocity over the past '" + PARAM_MINOR_AVERAGE_DAYS + "' days" +
            " exceeds the average velocity over the past '" + PARAM_MAJOR_AVERAGE_DAYS + "' days " +
            " by a factor of '" + PARAM_VELOCITY_FACTOR + "'";

    }

    public List<Parameter> getParameters() {
        return PARAMETERS;
    }

    @Transactional
    @Override
    public List<AuditIssue> run(AuditReportConfig reportConfig) {
        log.info("Running Outgoing Value Limits Report [userId: {}, reportName: {}]",
            reportConfig.getUserId(), reportConfig.getName());

        long majorAverageDays = reportConfig.getLong(PARAM_MAJOR_AVERAGE_DAYS)
            .orElse(PARAM_MAJOR_AVERAGE_DAYS_DEFAULT);
        long minorAverageDays = reportConfig.getLong(PARAM_MINOR_AVERAGE_DAYS)
            .orElse(PARAM_MINOR_AVERAGE_DAYS_DEFAULT);
        Double velocityFactor = reportConfig.getDouble(PARAM_VELOCITY_FACTOR)
            .orElse(PARAM_VELOCITY_FACTOR_DEFAULT);

        Instant majorStartDate = Instant.now().minus(Duration.ofDays(majorAverageDays)).truncatedTo(ChronoUnit.DAYS);
        Instant minorStartDate = Instant.now().minus(Duration.ofDays(minorAverageDays)).truncatedTo(ChronoUnit.DAYS);
        Instant endDate = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

        // gather transactions from the report source
        List<AccountTransaction> transactions = getReportTransactions(reportConfig, majorStartDate, endDate);

        // get the IDs for existing transactions with issues for this report
        Set<UUID> existingIssues = auditIssueRepository.listTransactionIds(reportConfig.getId());

        // count the outgoing transactions over the minor and major average periods
        Long[] counts = transactions.stream()
            .parallel()
            .filter(t -> t.getAmount().getAmount() < 0)
            .collect(
                () -> new Long[]{0L, 0L},
                (acc, t) -> {
                    acc[0]++;
                    if (t.getBookingDateTime().compareTo(minorStartDate) >= 0) {
                        acc[1]++;
                    }
                },
                (a, b) -> {
                    a[0] += b[0];
                    a[1] += b[1];
                }
            );

        // calculate the Major and Minor Average Velocity of outgoing transactions
        long majorAverage = counts[0] / majorAverageDays;
        long minorAverage = counts[1] / minorAverageDays;

        // calculate the threshold velocity for outgoing transactions to be considered an issue
        double velocityThreshold = majorAverage * velocityFactor;

        log.debug("Report factors [userId: {}, reportName: {}, majorVelocity: {}, minorVelocity: {}, threshold: {}]",
            reportConfig.getUserId(), reportConfig.getName(), majorAverage, minorAverage, velocityThreshold);

        // if the minor average velocity meets, or exceeds, the threshold
        List<AuditIssue> issues = List.of();
        if (minorAverage >= velocityThreshold) {
            // report all outgoing transactions within the minor average period that have not already been reported
            issues = transactions.stream()
                .filter(t -> t.getAmount().getAmount() < 0)
                .filter(t -> t.getBookingDateTime().compareTo(minorStartDate) >= 0)
                .filter(t -> !existingIssues.contains(t.getId()))
                .peek(t -> log.debug("New issue found [userId: {}, reportName: {}, transactionId: {}, value: {}]",
                    reportConfig.getUserId(), reportConfig.getName(), t.getId(), t.getAmount().getAmount()))
                .map(t -> AuditIssue.issueFor(reportConfig, t))
                .toList();
        }

        log.info("Completed Outgoing Velocity Report [userId: {}, reportName: {}, issuesFound: {}]",
            reportConfig.getUserId(), reportConfig.getName(), issues.size());
        return issues;
    }
}
