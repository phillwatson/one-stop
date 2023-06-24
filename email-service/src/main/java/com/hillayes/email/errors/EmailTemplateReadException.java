package com.hillayes.email.errors;

import com.hillayes.email.config.TemplateName;
import com.hillayes.exception.MensaException;

public class EmailTemplateReadException extends MensaException {
    public EmailTemplateReadException(TemplateName templateName,
                                      Throwable cause) {
        super(EmailErrorCodes.EMAIL_TEMPLATE_READ, cause);
        addParameter("templateName", templateName);
    }
}
