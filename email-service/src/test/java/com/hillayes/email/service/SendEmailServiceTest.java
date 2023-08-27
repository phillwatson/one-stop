package com.hillayes.email.service;

import com.hillayes.email.config.TemplateName;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.mockito.MockitoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import org.mockito.ArgumentCaptor;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.CreateSmtpEmail;
import sibModel.SendSmtpEmail;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SendEmailServiceTest {
    @InjectMock
    TransactionalEmailsApi emailApi;

    @Inject
    SendEmailService fixture;

    @BeforeEach
    public void beforeEach() throws ApiException {
        when(emailApi.sendTransacEmail(any()))
            .thenReturn(new CreateSmtpEmail().messageId("mock-message-id"));
    }

    @Test
    public void testSendEmail() throws Exception {
        SendEmailService.Recipient recipient =
            new SendEmailService.Recipient("mock@work.com", "Mr Mock", Locale.ENGLISH);

        Map<String, Object> params = Map.of(
            "acknowledge_uri", "http://validate?token=274768712uefhdsuihs78eyrf08723y4r",
            "expires", Instant.now().toString()
        );
        fixture.sendEmail(TemplateName.USER_REGISTERED, recipient, params);

        ArgumentCaptor<SendSmtpEmail> emailCapture = ArgumentCaptor.forClass(SendSmtpEmail.class);
        verify(emailApi).sendTransacEmail(emailCapture.capture());

        SendSmtpEmail email = emailCapture.getValue();
        assertTrue(email.getHtmlContent().contains(params.get(recipient.getEmail()).toString()));
        assertTrue(email.getHtmlContent().contains(params.get("acknowledge_uri").toString()));
        assertTrue(email.getHtmlContent().contains(params.get("expires").toString()));
    }
}
