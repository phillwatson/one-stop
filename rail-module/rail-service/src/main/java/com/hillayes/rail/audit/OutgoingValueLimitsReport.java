package com.hillayes.rail.audit;

import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.CategoryGroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.function.UnaryOperator.identity;

@ApplicationScoped
@Transactional
@Slf4j
public class OutgoingValueLimitsReport implements AuditReportTemplate {
    // the number of days over which the average outgoing transaction amount is calculated
    private static final String PARAM_AVERAGE_DAYS = "averageDays";
    private static final Long PARAM_AVERAGE_DAYS_DEFAULT = 30L;

    // the number of days over which the outgoing transactions are analysed for issues
    private static final String PARAM_REPORT_DAYS = "reportDays";
    private static final Long PARAM_REPORT_DAYS_DEFAULT = 5L;

    // the factor by which the average outgoing transaction amount is multiplied to
    // calculate the threshold for an outgoing transaction to be considered an issue
    private static final String PARAM_THRESHOLD_FACTOR = "thresholdFactor";
    private static final Double PARAM_THRESHOLD_FACTOR_DEFAULT = 1.5;

    // a list of parameters that the user can set when running this report
    private static final List<Parameter> PARAMETERS = List.of(
        new Parameter(PARAM_AVERAGE_DAYS, "The number of days over which the average outgoing transaction amount is calculated",
            ParameterType.LONG, PARAM_AVERAGE_DAYS_DEFAULT.toString()),
        new Parameter(PARAM_REPORT_DAYS, "The number of days over which the outgoing transactions are analysed for issues",
            ParameterType.LONG, PARAM_REPORT_DAYS_DEFAULT.toString()),
        new Parameter(PARAM_THRESHOLD_FACTOR, "The factor by which the average outgoing transaction amount is multiplied to calculate the threshold for an outgoing transaction to be considered an issue",
            ParameterType.DOUBLE, PARAM_THRESHOLD_FACTOR_DEFAULT.toString())
    );

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    @Inject
    CategoryGroupRepository categoryGroupRepository;

    @Override
    public String getId() {
        return "outgoing-value-limits";
    }

    public List<Parameter> getParameters() {
        return PARAMETERS;
    }

    @Override
    public List<String> run(AuditReportConfig reportConfig) {
        log.info("Running Outgoing Value Limits Report [userId: {}, reportName: {}]",
            reportConfig.getUserId(), reportConfig.getName());

        Long averageDays = reportConfig.getLong(PARAM_AVERAGE_DAYS)
            .orElse(PARAM_AVERAGE_DAYS_DEFAULT);
        Long reportDays = reportConfig.getLong(PARAM_REPORT_DAYS)
            .orElse(PARAM_REPORT_DAYS_DEFAULT);
        Double thresholdFactor = reportConfig.getDouble(PARAM_THRESHOLD_FACTOR)
            .orElse(PARAM_THRESHOLD_FACTOR_DEFAULT);

        Instant startDate = Instant.now().minus(Duration.ofDays(averageDays)).truncatedTo(ChronoUnit.DAYS);
        Instant endDate = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

        // gather transactions from the report source
        List<AccountTransaction> transactions = switch (reportConfig.getReportSource()) {
            case ACCOUNT -> accountTransactionRepository.findByAccount(
                reportConfig.getUserId(), reportConfig.getReportSourceId(), startDate, endDate);

            case CATEGORY_GROUP -> findByCategoryGroup(reportConfig, startDate, endDate);

            case CATEGORY -> accountTransactionRepository.findByCategory(
                reportConfig.getUserId(), reportConfig.getReportSourceId(), startDate, endDate);
        };

        // calculate the average outgoing transaction amount
        List<String> issues = transactions.stream()
            .filter(t -> t.getAmount().getAmount() < 0)
            .mapToDouble(t -> t.getAmount().getAmount())
            .average()
            .stream().mapToObj(average -> {
                // calculate the threshold for an outgoing transaction to be considered an issue
                // the value will be negative, as it is outgoing transactions
                double threshold = average * thresholdFactor;

                // does any outgoing transaction within the report days exceed the audit threshold
                Instant inclDate = Instant.now().minus(Duration.ofDays(reportDays)).truncatedTo(ChronoUnit.DAYS);
                return transactions.stream()
                    .filter(t -> t.getAmount().getAmount() < 0)
                    .filter(t -> t.getBookingDateTime().compareTo(inclDate) >= 0)
                    .filter(t -> t.getAmount().getAmount() <= threshold)
                    .peek(t -> log.debug("Outgoing Value Limits Report Issue Found [userId: {}, reportName: {}, transactionId: {}, value: {}]",
                        reportConfig.getUserId(), reportConfig.getName(), t.getId(), t.getAmount().getAmount()))
                    .map(t ->
                        String.format("Transaction %s has an outgoing value of %s, which exceeds the threshold of %s",
                            t.getId(), t.getAmount().getAmount(), threshold)
                    );
            }).flatMap(identity()).toList();

        log.info("Completed Outgoing Value Limits Report [userId: {}, reportName: {}, issuesFound: {}]",
            reportConfig.getUserId(), reportConfig.getName(), issues.size());
        return issues;
    }

    private List<AccountTransaction> findByCategoryGroup(AuditReportConfig reportConfig,
                                                         Instant startDate, Instant endDate) {
        return categoryGroupRepository.findByIdOptional(reportConfig.getReportSourceId())
            .map(categoryGroup -> accountTransactionRepository.findByCategoryGroup(categoryGroup,
                startDate, endDate, reportConfig.isUncategorisedIncluded()))
            .orElse(List.of());
    }
}
