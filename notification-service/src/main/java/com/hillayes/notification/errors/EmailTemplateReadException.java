package com.hillayes.notification.errors;

import com.hillayes.notification.config.TemplateName;
import com.hillayes.exception.MensaException;

public class EmailTemplateReadException extends MensaException {
    public EmailTemplateReadException(TemplateName templateName,
                                      Throwable cause) {
        super(ErrorCodes.EMAIL_TEMPLATE_READ, cause);
        addParameter("templateName", templateName);
    }
}
