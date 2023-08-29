package com.hillayes.email.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

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

    /**
     * Holds the common arguments, including details of the company owning the company.
     * These are available to all email templates.
     */
    Map<String,String> commonArgs();

    Corresponder defaultSender();

    /**
     * The API key used to authenticate with the 3rd-party email service provider.
     */
    String apiKey();

    /**
     * The URL on which the 3rd-party email service provider is located.
     */
    Optional<String> serviceUrl();

    /**
     * The configuration for each email template configuration, keyed by their name.
     */
    Map<TemplateName, TemplateConfig> templates();

    /**
     * The configuration for a single email topic.
     */
    interface TemplateConfig {
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
         * A collection of email templates keyed on their locale. The email sender will
         * select the template that best suits the recipient's locale/language.
         */
        @WithParentName()
        Map<Locale, LocaleTemplate> templates();
    }

    /**
     * Identifies the templates for an email subject and body.
     */
    interface LocaleTemplate {
        /**
         * The subject of the email.
         */
        String subject();

        /**
         * The path to the file containing the text used to render the email body.
         */
        String template();
    }

    interface Corresponder {
        /**
         * The name of the sender; normally "one-stop info".
         */
        @WithName("name")
        String getName();

        /**
         * The email address of the sender. Normally "info@one-stop.co.uk"
         */
        @WithName("email")
        String getEmail();

        @WithName("locale")
        Optional<Locale> getLocale();
    }
}
