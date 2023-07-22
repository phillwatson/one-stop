package com.hillayes.email.event.consumer;

import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import com.hillayes.email.repository.UserRepository;
import com.hillayes.email.service.SendEmailService;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserRegistered;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserTopicConsumer_UserRegisteredTest {
    @InjectMock
    UserRepository userRepository;

    @InjectMock
    SendEmailService sendEmailService;

    @Inject
    UserTopicConsumer fixture;

    @BeforeEach
    public void beforeEach() {
        when(userRepository.save(any())).then(invocation ->
            invocation.getArgument(0)
        );

        when(userRepository.save(any())).then(invocation ->
            invocation.getArgument(0)
        );
    }

    @Test
    public void testUserRegistered() {
        // given: a UserRegistered event
        UserRegistered event = UserRegistered.builder()
            .email(randomAlphanumeric(30))
            .locale(Locale.CANADA_FRENCH)
            .expires(LocalDateTime.now().plusMinutes(15).toInstant(ZoneOffset.UTC))
            .acknowledgerUri(URI.create("https://onestop/onboard?token=1234234"))
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailService).sendEmail(eq(TemplateName.USER_REGISTERED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(event.getEmail(), recipientCaptor.getValue().name());
        assertEquals(event.getEmail(), recipientCaptor.getValue().email());
        assertEquals(event.getLocale(), recipientCaptor.getValue().locale().get());

        // and: the email template parameters are taken from the event payload
        assertEquals(event.getAcknowledgerUri().toString(), paramsCaptor.getValue().get("acknowledge-uri"));
        assertEquals(fixture.format(event.getExpires()), paramsCaptor.getValue().get("expires"));
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
