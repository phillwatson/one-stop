package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserRegistered;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.repository.UserRepository;
import com.hillayes.notification.service.SendEmailService;
import com.hillayes.notification.task.SendEmailTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserTopicConsumer_UserRegisteredTest {
    @Mock
    UserRepository userRepository;

    @Mock
    SendEmailTask sendEmailTask;

    @InjectMocks
    UserTopicConsumer fixture;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

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
            .email(insecure().nextAlphanumeric(30))
            .locale(Locale.CANADA_FRENCH)
            .expires(LocalDateTime.now().plusMinutes(15).toInstant(ZoneOffset.UTC))
            .acknowledgerUri(URI.create("https://onestop/onboard?token=1234234"))
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<SendEmailService.Recipient> recipientCaptor = ArgumentCaptor.forClass(SendEmailService.Recipient.class);
        verify(sendEmailTask).queueTask(recipientCaptor.capture(), eq(TemplateName.USER_REGISTERED), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(event.getEmail(), recipientCaptor.getValue().getName());
        assertEquals(event.getEmail(), recipientCaptor.getValue().getEmail());
        assertEquals(event.getLocale(), recipientCaptor.getValue().getLocale().get());

        // and: the email template parameters are taken from the event payload
        assertEquals(event.getAcknowledgerUri().toString(), paramsCaptor.getValue().get("acknowledge_uri"));
        assertEquals(fixture.format(event.getExpires()), paramsCaptor.getValue().get("expires"));
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
