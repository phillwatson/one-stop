package com.hillayes.rail.audit;

import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;

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
public interface AuditReportTemplate {
    /**
     * The unique, descriptive identifier of this report template.
     */
    public String getName();

    /**
     * A brief description of what this report does.
     */
    public String getDescription();

    /**
     * Runs the report using the user's parameters declared in the given
     * AuditReportConfig.
     * @param reportConfig the configuration to be applied to this report.
     * @return a list of descriptive strings that detail each issue found in the report.
     */
    public List<AuditIssue> run(AuditReportConfig reportConfig);

    /**
     * Returns an ordered list of parameters that the user can set when
     * running this report.
     */
    public List<Parameter> getParameters();

    public record Parameter(
        String name,
        String description,
        ParameterType type,
        String defaultValue
    ) {}

    public enum ParameterType {
        STRING,
        LONG,
        DOUBLE,
        BOOLEAN
    }
}
