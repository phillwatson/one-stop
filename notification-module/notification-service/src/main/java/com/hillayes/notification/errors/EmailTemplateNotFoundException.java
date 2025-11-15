package com.hillayes.notification.errors;

import com.hillayes.notification.config.TemplateName;
import com.hillayes.exception.MensaException;

public class EmailTemplateNotFoundException extends MensaException {
    public EmailTemplateNotFoundException(TemplateName templateName) {
        this(templateName, null);
    }

    public EmailTemplateNotFoundException(TemplateName templateName,
                                          Throwable cause) {
        super(ErrorCodes.EMAIL_TEMPLATE_NOT_FOUND, cause);
        addParameter("templateName", templateName);
    }
}
