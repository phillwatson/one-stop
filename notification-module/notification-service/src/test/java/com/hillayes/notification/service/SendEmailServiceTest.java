package com.hillayes.notification.service;

import com.hillayes.email.api.EmailProviderApi;
import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.errors.SendEmailException;
import com.hillayes.notification.repository.TemplateRepository;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SendEmailServiceTest {
    private final List<EmailProviderApi> emailProviders = List.of(mock(), mock());
    private final TemplateRepository templateRepository = mock();
    private final EmailConfiguration configuration = mock();

    private final SendEmailService fixture = new SendEmailService(
        configuration,
        templateRepository,
        mockInstance(emailProviders)
    );

    @BeforeEach
    public void beforeEach() {
        emailProviders.forEach(emailApi ->
            when(emailApi.sendEmail(any())).thenReturn("mock-message-id")
        );
    }

    @Test
    public void testSendEmail() {
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

        // then: the first email service is called
        verify(emailProviders.getFirst()).sendEmail(any());

        // and: the second one is not called
        verifyNoInteractions(emailProviders.get(1));

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

    @Test
    public void testSendEmail_WithSingleFailure() {
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

        // and: the first email provider cannot send the email
        when(emailProviders.getFirst().sendEmail(any())).thenThrow(new RuntimeException("Mock failure"));

        // when: the email is sent
        fixture.sendEmail(TemplateName.USER_REGISTERED, recipient, params);

        // then: both email providers are called - as the first one failed
        emailProviders.forEach(emailApi ->
            verify(emailApi).sendEmail(any())
        );
    }

    @Test
    public void testSendEmail_WithAllFailure() {
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

        // and: the NO email provider can send the email
        emailProviders.forEach(emailApi ->
            when(emailApi.sendEmail(any())).thenThrow(new RuntimeException("Mock failure"))
        );

        // when: the email is sent
        // then: a exception is raised
        assertThrows(SendEmailException.class, () ->
            fixture.sendEmail(TemplateName.USER_REGISTERED, recipient, params)
        );

        // and: both email providers were tried
        emailProviders.forEach(emailApi ->
            verify(emailApi).sendEmail(any())
        );
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

    private <T> Instance<T> mockInstance(Collection<T> value) {
        Instance<T> mock = mock(Instance.class);
        when(mock.stream()).thenReturn(value.stream());
        return mock;
    }
}
