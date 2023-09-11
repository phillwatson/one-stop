package com.hillayes.notification.config;

import com.hillayes.notification.domain.NotificationId;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import java.util.Locale;
import java.util.Map;

/**
 * A configuration record for the notification service.
 */
@ConfigMapping(prefix = "one-stop.notification")
public interface NotificationConfiguration {
    /**
     * Holds the common arguments, including details of the company owning the company.
     * These are available to all notification messages.
     */
    Map<String,String> commonArgs();

    /**
     * The configuration for each notification message configuration, keyed by their ID.
     */
    Map<NotificationId, MessageConfig> messages();

    /**
     * The configuration for a single notification message.
     */
    interface MessageConfig {
        /**
         * A collection of notification messages keyed on their locale. The message
         * that best suits the user's locale/language will be selected.
         */
        @WithParentName()
        Map<Locale, String> templates();
    }
}
