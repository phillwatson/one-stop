package com.hillayes.email.errors;

import com.hillayes.email.config.TemplateName;
import com.hillayes.exception.MensaException;

public class EmailTemplateNotFoundException extends MensaException {
    public EmailTemplateNotFoundException(TemplateName templateName) {
        super(EmailErrorCodes.FAILED_TO_SEND_EMAIL);
        addParameter("templateName", templateName);
    }
}
