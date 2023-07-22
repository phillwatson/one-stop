package com.hillayes.email.service;

import com.hillayes.commons.net.Network;
import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import com.hillayes.email.errors.SendEmailException;
import com.hillayes.email.repository.TemplateRepository;
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
            params.put("host_ip", Network.getMyIpAddress());
            params.put("recipient_name", recipient.name());
            params.put("recipient_email", recipient.email());
            params.put("YEAR", LocalDate.now().getYear());
            params.put("COMPANY_COPYRIGHT_NAME", configuration.corporation().copyrightName());
            params.put("COMPANY_LONG_NAME", configuration.corporation().name());
            params.put("COMPANY_CONTACT_EMAIL", configuration.corporation().supportEmail());
            params.put("COMPANY_ADDRESS_LINE1", configuration.corporation().address().addressLine1());
            params.put("COMPANY_ADDRESS_LINE2", configuration.corporation().address().addressLine2());
            params.put("COMPANY_ADDRESS_LINE3", configuration.corporation().address().addressLine3());

            String subject = templateRepository.renderSubject(templateName, params, recipient.locale());
            params.put("SUBJECT", subject);

            String content = templateRepository.renderTemplate(templateName, params, recipient.locale());
            params.put("__TEMPLATE_CONTENT__", content);

            String body = templateRepository.renderTemplate(TemplateName.HEADER, params, recipient.locale());

            SendSmtpEmail email = new SendSmtpEmail()
                .sender(new SendSmtpEmailSender()
                    .name(templateConfig.sender().orElse(configuration.defaultSender()).name())
                    .email(templateConfig.sender().orElse(configuration.defaultSender()).email()))
                .addToItem(new SendSmtpEmailTo()
                    .name(recipient.name())
                    .email(recipient.email()))
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
            this.email = user.getEmail();
            this.name = user.getPreferredName();
            this.locale = Optional.ofNullable(user.getLocale());
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String email() {
            return email;
        }

        @Override
        public Optional<Locale> locale() {
            return locale;
        }
    }
}
