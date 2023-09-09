package com.hillayes.notification.errors;

import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
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
