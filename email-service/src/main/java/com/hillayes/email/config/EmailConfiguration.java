package com.hillayes.email.config;

import io.smallrye.config.ConfigMapping;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A configuration record for the email service.
 */
@ConfigMapping(prefix = "one-stop.email")
public interface EmailConfiguration {
    /**
     * Whether the sending of emails is disabled.
     */
    boolean disabled();

    Corresponder defaultSender();

    /**
     * The API key used to authenticate with the email service provider.
     */
    String apiKey();

    /**
     * The configuration for each email template configuration, keyed by their name.
     */
    Map<TemplateName, TemplateConfig> templates();

    /**
     * The configuration for a single email topic.
     */
    interface TemplateConfig {
        /**
         * The subject of the email.
         */
        String subject();

        /**
         * The sender of the email. If not provided, the defaultSender applies.
         */
        Optional<Corresponder> sender();

        /**
         * The optional receiver of the email. If not present, it is assumed the receiver
         * will be identified by the context (e.g. user record).
         */
        Optional<Corresponder> receiver();

        /**
         * The path to the file containing the text used to render the email body.
         * During template look-up, the path may be augmented with the email recipient's
         * locale.
         */
        String template();
    }

    interface Corresponder {
        /**
         * The name of the sender; normally "one-stop info".
         */
        String name();

        /**
         * The email address of the sender. Normally "info@one-stop.co.uk"
         */
        String email();

        Optional<Locale> locale();
    }
}
