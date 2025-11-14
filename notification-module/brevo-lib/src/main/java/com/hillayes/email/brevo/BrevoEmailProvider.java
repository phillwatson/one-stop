package com.hillayes.email.brevo;

import com.hillayes.email.api.EmailProviderApi;
import com.hillayes.email.api.domain.SendEmail;
import com.hillayes.email.brevo.api.SendEmailApi;
import com.hillayes.email.brevo.api.domain.BrevoEmail;
import com.hillayes.email.brevo.api.domain.BrevoEmailResponse;
import com.hillayes.email.brevo.api.domain.BrevoRecipient;
import com.hillayes.email.brevo.api.domain.BrevoSender;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@Priority(50)
@Slf4j
public class BrevoEmailProvider implements EmailProviderApi {
    @Inject
    @RestClient
    SendEmailApi sendEmailApi;

    @Override
    public String sendEmail(SendEmail sendEmail) {
        log.debug("Sending email [subject: {}]", sendEmail.getSubject());

        BrevoEmailResponse response = sendEmailApi.sendEmail(BrevoEmail.builder()
            .sender(BrevoSender.builder()
                .name(sendEmail.getSender().getName())
                .email(sendEmail.getSender().getEmail())
                .build())
            .to(sendEmail.getTo().stream()
                .map(recipient ->
                    BrevoRecipient.builder()
                        .name(recipient.getName())
                        .email(recipient.getEmail())
                        .build()
                ).toList()
            )
            .cc(sendEmail.getCc().stream()
                .map(recipient ->
                    BrevoRecipient.builder()
                        .name(recipient.getName())
                        .email(recipient.getEmail())
                        .build()
                ).toList()
            )
            .bcc(sendEmail.getBcc().stream()
                .map(recipient ->
                    BrevoRecipient.builder()
                        .name(recipient.getName())
                        .email(recipient.getEmail())
                        .build()
                ).toList()
            )
            .subject(sendEmail.getSubject())
            .htmlContent(sendEmail.getHtmlContent())
            .build()
        );

        return response.getMessageId();
    }
}
