package com.hillayes.rail.audit;

import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.CategoryGroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

/**
 * An audit report template is a predefined report that can be run by users to
 * analyze their transactions. Each template has a unique identifier and a
 * run method that takes an AuditReportConfig as a parameter. The configuration
 * object contains the user's settings for the report.
 * <p/>
 * Users can create any number of reports based on these templates and customize
 * them with their own parameter values. The reports can then apply those parameters
 * to their transaction audit algorithms.
 */
@ApplicationScoped
public abstract class AuditReportTemplate {
    @Inject
    AccountTransactionRepository accountTransactionRepository;

    @Inject
    CategoryGroupRepository categoryGroupRepository;

    /**
     * The unique, descriptive identifier of this report template.
     */
    public abstract String getName();

    /**
     * A brief description of what this report does.
     */
    public abstract String getDescription();

    /**
     * Runs the report using the user's parameters declared in the given
     * AuditReportConfig, returning a list of issues identified by the report.
     * If the report finds no issues, it should return an empty list.
     *
     * @param reportConfig the configuration to be applied to this report.
     * @return a list of issues identified by the report.
     */
    public abstract List<AuditIssue> run(AuditReportConfig reportConfig);

    /**
     * Returns an ordered list of parameters that the user can set when
     * running this report. If the report takes no parameters, return an empty
     * list.
     */
    public abstract List<Parameter> getParameters();

    /**
     * Returns the list of transactions identified by the report configuration's
     * source. The transactions are filtered by the given start and end dates,
     * and ordered by date in ascending order.
     *
     * @param reportConfig the configuration of the report
     * @param startDate the start date of the transactions
     * @param endDate the end date of the transactions
     * @return a list of transactions that match the report source configuration
     */
    protected List<AccountTransaction> getReportTransactions(AuditReportConfig reportConfig,
                                                             Instant startDate, Instant endDate) {
        // gather transactions from the report source
        return switch (reportConfig.getReportSource()) {
            case ALL -> accountTransactionRepository.findByUser(
                reportConfig.getUserId(), startDate, endDate);

            case ACCOUNT -> accountTransactionRepository.findByAccount(
                reportConfig.getUserId(), reportConfig.getReportSourceId(), startDate, endDate);

            case CATEGORY_GROUP -> categoryGroupRepository.findByIdOptional(reportConfig.getReportSourceId())
                .map(categoryGroup -> accountTransactionRepository.findByCategoryGroup(categoryGroup,
                    startDate, endDate, reportConfig.isUncategorisedIncluded()))
                .orElse(List.of());

            case CATEGORY -> accountTransactionRepository.findByCategory(
                reportConfig.getUserId(), reportConfig.getReportSourceId(), startDate, endDate);
        };
    }

    public record Parameter(
        String name,
        String description,
        ParameterType type,
        String defaultValue
    ) { }

    public enum ParameterType {
        STRING,
        LONG,
        DOUBLE,
        BOOLEAN
    }
}
