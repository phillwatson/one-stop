package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.domain.AuditReportConfig;

public class AuditReportConfigAlreadyExistsException extends MensaException {
    public AuditReportConfigAlreadyExistsException(AuditReportConfig reportConfig) {
        super(RailsErrorCodes.AUDIT_REPORT_CONFIG_ALREADY_EXISTS);
        addParameter("id", reportConfig.getId());
        addParameter("name", reportConfig.getName());
    }
}
