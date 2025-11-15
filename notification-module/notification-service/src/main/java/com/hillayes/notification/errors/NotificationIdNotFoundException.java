package com.hillayes.notification.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.notification.domain.NotificationId;

public class NotificationIdNotFoundException extends MensaException {
    public NotificationIdNotFoundException(NotificationId notificationId) {
        this(notificationId, null);
    }

    public NotificationIdNotFoundException(NotificationId notificationId,
                                           Throwable cause) {
        super(ErrorCodes.NOTIFICATION_ID_NOT_FOUND, cause);
        addParameter("notificationId", notificationId);
    }
}
