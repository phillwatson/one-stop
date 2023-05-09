package com.hillayes.email.service;

import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SendEmailService {
    private final EmailConfiguration configuration;

    public void sendEmail(TemplateName templateName,
                          EmailConfiguration.Corresponder recipient,
                          Map<String,Object> params) throws ApiException, IOException {
        if (configuration.disabled()) {
            log.debug("Email sending is disabled");
            return;
        }

        EmailConfiguration.TemplateConfig templateConfig = configuration.templates().get(templateName);
        if (templateConfig == null) {
            log.error("No template config found");
            return;
        }

        if ((recipient == null) && (templateConfig.receiver().isEmpty())) {
            log.error("No recipient found");
            return;
        }

        SendSmtpEmail email = new SendSmtpEmail()
            .sender(new SendSmtpEmailSender()
                .name(templateConfig.sender().orElse(configuration.defaultSender()).name())
                .email(templateConfig.sender().orElse(configuration.defaultSender()).email()))
            .addToItem(templateConfig.receiver()
                .map(config -> new SendSmtpEmailTo()
                    .name(config.name())
                    .email(config.email()))
                .orElse(new SendSmtpEmailTo()
                    .name(recipient.name())
                    .email(recipient.email())))
            .subject(templateConfig.subject());

        if (params != null) {
            email.params(params);
        }

        if (templateConfig.template().isPresent()) {
            email.htmlContent(readHtml(templateConfig.template().get()));
        } else if (templateConfig.templateId().isPresent()) {
            email.templateId(Long.valueOf(templateConfig.templateId().get()));
        } else {
            email.htmlContent("<html><body><p>Hi {{contact.name}},</p><p>How are you?</p></body></html>");
        }

        TransactionalEmailsApi emailApi = new TransactionalEmailsApi();
        emailApi.getApiClient().setApiKey(configuration.apiKey());
        emailApi.sendTransacEmail(email);
    }

    private String readHtml(String path) throws IOException {
        InputStream resource = this.getClass().getResourceAsStream("templates/" + path);
        if (resource == null) {
            throw new IOException("Template not found: " + path);
        }

        try (InputStream content = resource) {
            try (Scanner scanner = new Scanner(content, StandardCharsets.UTF_8)) {
                StringBuilder result = new StringBuilder();
                while (scanner.hasNextLine()) {
                    result.append(scanner.nextLine()).append('\n');
                }
                return result.toString();
            }
        }
    }
}
