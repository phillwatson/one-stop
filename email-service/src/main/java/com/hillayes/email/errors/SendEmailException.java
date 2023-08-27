package com.hillayes.email.errors;

import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import com.hillayes.exception.MensaException;

public class SendEmailException extends MensaException {
    public SendEmailException(TemplateName templateName,
                              EmailConfiguration.Corresponder recipient,
                              Throwable aCause) {
        super(EmailErrorCodes.FAILED_TO_SEND_EMAIL, aCause);
        addParameter("templateName", templateName);
        addParameter("email", recipient.getEmail());
    }
}
