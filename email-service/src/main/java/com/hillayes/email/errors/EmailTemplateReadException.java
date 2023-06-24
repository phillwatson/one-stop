package com.hillayes.email.errors;

import com.hillayes.email.config.TemplateName;
import com.hillayes.exception.MensaException;

public class EmailTemplateReadException extends MensaException {
    public EmailTemplateReadException(TemplateName templateName) {
        super(EmailErrorCodes.EMAIL_TEMPLATE_READ);
        addParameter("templateName", templateName);
    }
}
