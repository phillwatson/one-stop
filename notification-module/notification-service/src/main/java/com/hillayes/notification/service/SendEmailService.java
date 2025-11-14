package com.hillayes.notification.service;

import com.hillayes.commons.Strings;
import com.hillayes.commons.net.Network;
import com.hillayes.email.api.EmailProviderApi;
import com.hillayes.email.api.domain.EmailRecipient;
import com.hillayes.email.api.domain.EmailSender;
import com.hillayes.email.api.domain.SendEmail;
import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.errors.SendEmailException;
import com.hillayes.notification.repository.TemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SendEmailService {
    private final EmailConfiguration configuration;
    private final TemplateRepository templateRepository;
    private final Instance<EmailProviderApi> emailProviders;

    public void sendEmail(TemplateName templateName,
                          EmailConfiguration.Corresponder recipient,
                          Map<String, Object> params) throws SendEmailException {
        log.debug("Sending email [template: {}]", templateName);
        if (configuration.disabled()) {
            log.debug("SendEmail sending is disabled");
            return;
        }

        EmailConfiguration.TemplateConfig templateConfig = configuration.templates().get(templateName);
        if (templateConfig == null) {
            log.error("No template config found [templateName: {}]", templateName);
            return;
        }

        if (recipient == null) {
            recipient = templateConfig.recipient().orElse(null);
        }
        if (recipient == null) {
            log.error("No recipient found [templateName: {}]", templateName);
            return;
        }

        if (Strings.isBlank(recipient.getEmail())) {
            log.warn("EmailRecipient has no email address [templateName: {}]", templateName);
            return;
        }

        try {
            // make a mutable map of parameters
            params = new HashMap<>(params);

            // add common context parameters
            params.putAll(configuration.commonArgs());
            params.put("host_ip", Network.getMyIpAddress());
            params.put("recipient", recipient);
            params.put("YEAR", LocalDate.now().getYear());

            String subject = templateRepository.renderSubject(templateName, params, recipient.getLocale());
            params.put("SUBJECT", subject);

            String content = templateRepository.renderTemplate(templateName, params, recipient.getLocale());
            params.put("__TEMPLATE_CONTENT__", content);

            String body = templateRepository.renderTemplate(TemplateName.HEADER, params, recipient.getLocale());

            SendEmail email = SendEmail.builder()
                .sender(EmailSender.builder()
                    .name(templateConfig.sender().orElseGet(configuration::defaultSender).getName())
                    .email(templateConfig.sender().orElseGet(configuration::defaultSender).getEmail())
                    .build()
                )
                .to(List.of(EmailRecipient.builder()
                        .name(recipient.getName())
                        .email(recipient.getEmail())
                        .build()
                    )
                )
                .subject(subject)
                .htmlContent(body)
                .build();

            log.trace("Sending email [template: {}, {}]", templateName, email);
            String messageId = emailProviders.stream()
                .map(emailProvider -> {
                    try {
                        return emailProvider.sendEmail(email);
                    } catch (Exception e) {
                        log.warn("Failed to send email [provider: {}]", emailProvider, e);
                        return (String)null;
                    }
                })
                .filter(Strings::isNotBlank)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("All email providers failed to send email"));

            log.debug("Sent email [template: {}, id: {}]", templateName, messageId);
        } catch (Exception e) {
            throw new SendEmailException(templateName, recipient, e);
        }
    }

    public static class Recipient implements EmailConfiguration.Corresponder {
        private final String email;
        private final String name;
        private final Locale locale;

        public Recipient(User user) {
            this(user.getEmail(),
                user.getPreferredName() != null ? user.getPreferredName() : user.getGivenName(),
                user.getLocale());
        }

        public Recipient(String email, String name, Locale locale) {
            this.email = email;
            this.name = name;
            this.locale = locale;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public Optional<Locale> getLocale() {
            return Optional.ofNullable(locale);
        }
    }
}
