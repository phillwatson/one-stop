package com.hillayes.notification.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.notification.domain.NotificationId;

public class ParameterSerialisationException extends MensaException {
    public ParameterSerialisationException(NotificationId notificationId,
                                           Throwable cause) {
        super(ErrorCodes.PARAMETER_SERIALISATION_ERROR, cause);
        addParameter("notificationId", notificationId);
    }
}
