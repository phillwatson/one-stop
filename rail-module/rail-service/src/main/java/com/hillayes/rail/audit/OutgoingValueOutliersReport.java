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
 * A report will identify the recent outgoing transactions within selected accounts (or
 * categories) for which the value exceeds an average, based on the following parameters:
 * <ol>
 * <li>Minor Average Days - the days over which the recent transactions are averaged</li>
 * <li>Major Average Days - the days over which the overall transactions are averaged</li>
 * <li>Outlier Factor (double) - the multiplication factor by which the outliers are identified</li>
 * </ol>
 * With these parameters, the following values are calculated:
 * <ol>
 * <li>Minor Count - the number of outgoing transaction within the Minor Average Days</li>
 * <li>Minor Sum - the sum of outgoing transaction within the Minor Average Days</li>
 * <li>Major Count - the number of outgoing transaction within the Major Average Days</li>
 * <li>Major Sum - the sum of outgoing transaction within the Major Average Days</li>
 * <li>Minor Average Value = Minor Sum / Minor Count</li>
 * <li>Major Average Value = Major Sum / Major Count</li>
 * <li>Threshold = Major Average Value * Outlier Factor</li>
 * </ol>
 * If the Minor Average Value meets, or exceeds, the Threshold, an issue will be raised
 * for each outgoing transaction within the Minor Average Days.
 */
@ApplicationScoped
@Slf4j
public class OutgoingValueOutliersReport extends AuditReportTemplate {
    // the number of days over which the overall values are averaged
    private static final String PARAM_MAJOR_AVERAGE_DAYS = "majorAverageDays";
    private static final Long PARAM_MAJOR_AVERAGE_DAYS_DEFAULT = 30L;

    // the number of days over which the recent values are averaged
    private static final String PARAM_MINOR_AVERAGE_DAYS = "minorAverageDays";
    private static final Long PARAM_MINOR_AVERAGE_DAYS_DEFAULT = 5L;

    // the factor by which the average value is multiplied to calculate the
    // threshold for outgoing transactions to be considered an issue
    private static final String PARAM_OUTLIER_FACTOR = "outlierFactor";
    private static final Double PARAM_OUTLIER_FACTOR_DEFAULT = 1.5;

    // a list of parameters that the user can set when running this report
    private static final List<Parameter> PARAMETERS = List.of(
        new Parameter(PARAM_MAJOR_AVERAGE_DAYS, "The number of days over which the major average value is calculated",
            ParameterType.LONG, PARAM_MAJOR_AVERAGE_DAYS_DEFAULT.toString()),
        new Parameter(PARAM_MINOR_AVERAGE_DAYS, "The number of days over which the minor average value is calculated",
            ParameterType.LONG, PARAM_MINOR_AVERAGE_DAYS_DEFAULT.toString()),
        new Parameter(PARAM_OUTLIER_FACTOR,
            "The factor by which the major average value is multiplied to calculate the threshold" +
                " at which the minor average value is considered an issue",
            ParameterType.DOUBLE, PARAM_OUTLIER_FACTOR_DEFAULT.toString())
    );

    @Inject
    AuditIssueRepository auditIssueRepository;

    @Override
    public String getName() {
        return "outgoing-value-outliers";
    }

    @Override
    public String getDescription() {
        return "Detects outgoing transactions whose total average value over the past '" + PARAM_MINOR_AVERAGE_DAYS + "' days" +
            " exceeds the average over the past '" + PARAM_MAJOR_AVERAGE_DAYS + "' days " +
            " by a factor of '" + PARAM_OUTLIER_FACTOR + "'";
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
        Double outlierFactor = reportConfig.getDouble(PARAM_OUTLIER_FACTOR)
            .orElse(PARAM_OUTLIER_FACTOR_DEFAULT);

        Instant majorStartDate = Instant.now().minus(Duration.ofDays(majorAverageDays)).truncatedTo(ChronoUnit.DAYS);
        Instant minorStartDate = Instant.now().minus(Duration.ofDays(minorAverageDays)).truncatedTo(ChronoUnit.DAYS);
        log.debug("Report parameters [userId: {}, reportName: {}, majorVelocity: {}, minorVelocity: {}, factor: {}]",
            reportConfig.getUserId(), reportConfig.getName(), majorAverageDays, minorAverageDays, outlierFactor);

        // gather transactions from the report source
        Instant endDate = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
        List<AccountTransaction> transactions = getReportTransactions(reportConfig, majorStartDate, endDate);

        // get the IDs for existing transactions with issues for this report
        Set<UUID> existingIssues = auditIssueRepository.listTransactionIds(reportConfig.getId());

        // total the outgoing transactions over the minor and major average periods
        Long[] totals = transactions.stream()
            .parallel()
            .filter(t -> t.getAmount().getAmount() < 0)
            .collect(
                () -> new Long[]{0L, 0L},
                (acc, t) -> {
                    long transactionValue = -t.getAmount().getAmount();
                    acc[0] += transactionValue;
                    if (t.getBookingDateTime().compareTo(minorStartDate) >= 0) {
                        acc[1] += transactionValue;
                    }
                },
                (a, b) -> {
                    a[0] += b[0];
                    a[1] += b[1];
                }
            );

        // calculate the Major and Minor Average Value of outgoing transactions
        long majorAverage = totals[0] / majorAverageDays;
        long minorAverage = totals[1] / minorAverageDays;

        // calculate the threshold value for outgoing transactions to be considered an issue
        double valueThreshold = majorAverage * outlierFactor;

        log.debug("Report factors [userId: {}, reportName: {}, majorVelocity: {}, minorVelocity: {}, threshold: {}]",
            reportConfig.getUserId(), reportConfig.getName(), majorAverage, minorAverage, valueThreshold);

        // if the minor average value meets, or exceeds, the threshold
        List<AuditIssue> issues = List.of();
        if (minorAverage >= valueThreshold) {
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

        log.info("Completed Outgoing Value Report [userId: {}, reportName: {}, issuesFound: {}]",
            reportConfig.getUserId(), reportConfig.getName(), issues.size());
        return issues;
    }
}
