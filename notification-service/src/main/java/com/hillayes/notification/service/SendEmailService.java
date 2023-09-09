package com.hillayes.notification.service;

import com.hillayes.commons.net.Network;
import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.errors.SendEmailException;
import com.hillayes.notification.repository.TemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sibApi.TransactionalEmailsApi;
import sibModel.CreateSmtpEmail;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SendEmailService {
    private final EmailConfiguration configuration;
    private final TemplateRepository templateRepository;
    private final TransactionalEmailsApi emailApi;

    public void sendEmail(TemplateName templateName,
                          EmailConfiguration.Corresponder recipient) throws SendEmailException {
        sendEmail(templateName, recipient, null);
    }

    public void sendEmail(TemplateName templateName,
                          EmailConfiguration.Corresponder recipient,
                          Map<String, Object> params) throws SendEmailException {
        log.debug("Sending email [template: {}]", templateName);
        if (configuration.disabled()) {
            log.debug("Email sending is disabled");
            return;
        }

        EmailConfiguration.TemplateConfig templateConfig = configuration.templates().get(templateName);
        if (templateConfig == null) {
            log.error("No template config found [templateName: {}]", templateName);
            return;
        }

        if (recipient == null) {
            recipient = templateConfig.receiver().orElse(null);
        }
        if (recipient == null) {
            log.error("No recipient found [templateName: {}]", templateName);
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

            SendSmtpEmail email = new SendSmtpEmail()
                .sender(new SendSmtpEmailSender()
                    .name(templateConfig.sender().orElse(configuration.defaultSender()).getName())
                    .email(templateConfig.sender().orElse(configuration.defaultSender()).getEmail()))
                .addToItem(new SendSmtpEmailTo()
                    .name(recipient.getName())
                    .email(recipient.getEmail()))
                .subject(subject)
                .htmlContent(body);

            CreateSmtpEmail createSmtpEmail = emailApi.sendTransacEmail(email);
            log.debug("Sent email [template: {}, id: {}]", templateName, createSmtpEmail.getMessageId());
        } catch (Exception e) {
            throw new SendEmailException(templateName, recipient, e);
        }
    }

    public static class Recipient implements EmailConfiguration.Corresponder {
        private final String email;
        private final String name;
        private final Optional<Locale> locale;

        public Recipient(String email, String name, Locale locale) {
            this.email = email;
            this.name = name;
            this.locale = Optional.ofNullable(locale);
        }

        public Recipient(User user) {
            this(user.getEmail(), user.getPreferredName(), user.getLocale());
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
            return locale;
        }
    }
}
