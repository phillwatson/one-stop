package com.hillayes.email.service;

import com.hillayes.email.EmailConfiguration;
import com.hillayes.email.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class SendEmailService {
    @ConfigProperty(name = "one-stop.email.api-key")
    String apiKey;

    @ConfigProperty(name = "one-stop.email.disabled", defaultValue = "false")
    boolean disabled;

    public void sendEmail(EmailConfiguration.TemplateConfig templateConfig, User user) throws ApiException {
        if (disabled) {
            log.debug("Email sending is disabled");
            return;
        }

        if (templateConfig == null) {
            log.error("No template config found");
            return;
        }

        if ((user == null) && (templateConfig.receiver().isEmpty())) {
            log.error("No user or receiver found");
            return;
        }

        SendSmtpEmail email = new SendSmtpEmail()
            .sender(new SendSmtpEmailSender()
                .name(templateConfig.sender().name())
                .email(templateConfig.sender().email()))
            .addToItem(templateConfig.receiver()
                .map(config -> new SendSmtpEmailTo()
                    .name(config.name())
                    .email(config.email()))
                .orElse(new SendSmtpEmailTo()
                    .name(user.getPreferredName())
                    .email(user.getEmail())))
            .subject(templateConfig.subject())
            .htmlContent("<html><body><p>Hi {{params.name}},</p><p>How are you?</p></body></html>")
            .params(Map.of("name", user.getPreferredName()));

        TransactionalEmailsApi emailApi = new TransactionalEmailsApi();
        emailApi.getApiClient().setApiKey(apiKey);
        emailApi.sendTransacEmail(email);
    }
}
