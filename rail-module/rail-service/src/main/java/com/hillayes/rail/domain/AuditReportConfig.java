package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

/**
 * Audit report configurations are created by users to configure audit reports,
 * based on report templates, to analyse the transactions on their accounts
 * and/or categories.
 * Users can create any number of reports based on these templates and customize
 * them with their own parameter values. The reports can then apply those parameters
 * to their transaction audit algorithms.
 *
 * @see AuditReportParameter
 * @see com.hillayes.rail.audit.AuditReportTemplate
 */
@Entity(name = "audit_report_config")
@Getter
@Setter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AuditReportConfig {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Version
    @Column(name = "version")
    private long version;

    @Column(nullable = false)
    private boolean disabled;

    /**
     * The user to whom this report belongs.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The name of this report, as defined by the user.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String name;

    /**
     * A description of the report, as defined by the user.
     */
    private String description;

    /**
     * The source of the transactions to be analysed by the report.
     */
    @Column(name = "report_source", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportSource reportSource;

    /**
     * The ID of the source of the transactions to be analysed by the report.
     */
    @Column(name = "report_source_id", nullable = false)
    private UUID reportSourceId;

    /**
     * When the source of the transactions is a category group, this flag indicates
     * whether transactions that are not categorised should be included in the report.
     * The default is false.
     */
    @Column(name = "include_uncategorised", nullable = false)
    private boolean uncategorisedIncluded;

    /**
     * The report template that this report is based on. Taken from the name of
     * the com.hillayes.rail.audit.AuditReportTemplate instance.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "template_name", nullable = false)
    private String templateName;

    /**
     * The config properties for this report.
     */
    @OneToMany(mappedBy = "config", fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @MapKey(name = "name")
    @lombok.Builder.Default
    private Map<String, AuditReportParameter> parameters = new HashMap<>();

    /**
     * A convenience method for adding a configuration parameter to this report
     * configuration.
     * @param name The name of the configuration parameter.
     * @param value The value of the configuration parameter.
     * @return This report, for method chaining.
     */
    public AuditReportConfig addParameter(String name, String value) {
        parameters.put(name, AuditReportParameter.builder()
            .config(this)
            .name(name)
            .value(value)
            .build());
        return this;
    }

    /**
     * A convenience method for removing a configuration parameter from this report
     * configuration.
     * @param name The name of the configuration parameter.
     * @return This report, for method chaining.
     */
    public AuditReportConfig removeParameter(String name) {
        getParameters().remove(name);
        return this;
    }

    /**
     * Returns the parameter with the given name, if it exists.
     * @param name The name of the parameter.
     * @return The parameter, if it exists.
     */
    public Optional<AuditReportParameter> getParameter(String name) {
        return Optional.ofNullable(parameters.get(name));
    }

    /**
     * Returns the value of the parameter with the given name as a String.
     * @param name The name of the parameter.
     * @return The value of the parameter as a String.
     */
    public Optional<String> getString(String name) {
        return getParameter(name).map(AuditReportParameter::getValue);
    }

    /**
     * Returns the value of the parameter with the given name as a Long.
     * @param name The name of the parameter.
     * @return The value of the parameter as a Long.
     */
    public Optional<Long> getLong(String name) {
        return getString(name).map(Long::parseLong);
    }

    /**
     * Returns the value of the parameter with the given name as a Double.
     * @param name The name of the parameter.
     * @return The value of the parameter as a Double.
     */
    public Optional<Double> getDouble(String name) {
        return getString(name).map(Double::parseDouble);
    }

    /**
     * Identifies the source of the transactions to be analysed by the report.
     */
    public enum ReportSource {
        ACCOUNT, // an account belonging to the user
        CATEGORY_GROUP, // a group of categories belonging to the user
        CATEGORY; // a single category belonging to the user
    }
}
