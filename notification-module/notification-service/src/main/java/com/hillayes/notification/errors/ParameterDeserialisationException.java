package com.hillayes.notification.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.notification.domain.NotificationId;

public class ParameterDeserialisationException extends MensaException {
    public ParameterDeserialisationException(NotificationId notificationId,
                                             Throwable cause) {
        super(ErrorCodes.PARAMETER_DESERIALISATION_ERROR, cause);
        addParameter("notificationId", notificationId);
    }
}
