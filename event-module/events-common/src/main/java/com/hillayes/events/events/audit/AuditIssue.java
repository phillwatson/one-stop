package com.hillayes.events.events.audit;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public class AuditIssue {
    /**
     * The ID of the audit report in which the issue was detected.
     */
    private String auditReportId;

    /**
     * The name, as given by the user, of the audit report in which the issue was detected.
     */
    private String reportName;

    /**
     * The description, as given by the user, of the audit report in which the issue was detected.
     */
    private String reportDescription;

    /**
     * The parameters used to generate the report in which the issue was detected.
     */
    private Map<String,String> reportParameters;

    private List<String> issues;
}
