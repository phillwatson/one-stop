package com.hillayes.notification.service;

import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.CreateSmtpEmail;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SendEmailServiceTest {
    @Mock
    TransactionalEmailsApi emailApi;

    @Mock
    TemplateRepository templateRepository;

    @Mock
    EmailConfiguration configuration;

    @InjectMocks
    SendEmailService fixture;

    @BeforeEach
    public void beforeEach() throws ApiException {
        openMocks(this);

        when(emailApi.sendTransacEmail(any()))
            .thenReturn(new CreateSmtpEmail().messageId("mock-message-id"));
    }

    @Test
    public void testSendEmail() throws Exception {
        // given: an email configuration containing the required template
        when(configuration.disabled()).thenReturn(false);
        when(configuration.defaultSender()).thenReturn(mockCorresponder());
        when(configuration.templates()).thenReturn(Map.of(
            TemplateName.USER_REGISTERED, mockTemplateConfig())
        );

        // and: an email recipient
        SendEmailService.Recipient recipient =
            new SendEmailService.Recipient("mock@work.com", "Mr Mock", Locale.ENGLISH);

        // and: a collection of template parameters
        Map<String, Object> params = Map.of(
            "acknowledge_uri", "http://validate?token=274768712uefhdsuihs78eyrf08723y4r",
            "expires", Instant.now().toString()
        );

        // when: the email is sent
        fixture.sendEmail(TemplateName.USER_REGISTERED, recipient, params);

        // then: the email service is called
        verify(emailApi).sendTransacEmail(any());

        // and: the repository was called to render the template in the recipient's locale
        ArgumentCaptor<Map<String,Object>> templateCapture = ArgumentCaptor.forClass(Map.class);
        verify(templateRepository).renderTemplate(
            eq(TemplateName.USER_REGISTERED), templateCapture.capture(), eq(recipient.getLocale()));

        // and: the template parameters where passed to render the template content
        Map<String,Object> templateParams = templateCapture.getValue();
        assertEquals(recipient, templateParams.get("recipient"));
        assertEquals(params.get("acknowledge_uri"), templateParams.get("acknowledge_uri"));
        assertEquals(params.get("expires"), templateParams.get("expires"));
    }

    private EmailConfiguration.Corresponder mockCorresponder() {
        return new EmailConfiguration.Corresponder() {
            @Override
            public String getName() {
                return "Mock";
            }

            @Override
            public String getEmail() {
                return "mock@work.com";
            }

            @Override
            public Optional<Locale> getLocale() {
                return Optional.of(Locale.ENGLISH);
            }
        };
    }

    private EmailConfiguration.TemplateConfig mockTemplateConfig() {
        return new EmailConfiguration.TemplateConfig() {
            @Override
            public Optional<EmailConfiguration.Corresponder> sender() {
                return Optional.empty();
            }

            @Override
            public Optional<EmailConfiguration.Corresponder> recipient() {
                return Optional.empty();
            }

            @Override
            public Map<Locale, EmailConfiguration.LocaleTemplate> templates() {
                return Map.of(
                    Locale.ENGLISH, mockLocaleTemplate(),
                    Locale.FRENCH, mockLocaleTemplate()
                );
            }
        };
    }

    private EmailConfiguration.LocaleTemplate mockLocaleTemplate() {
        return new EmailConfiguration.LocaleTemplate() {
            @Override
            public String subject() {
                return "Hi $user.preferredName$, please complete your One-Stop registration";
            }

            @Override
            public String template() {
                return "";
            }
        };
    }
}
