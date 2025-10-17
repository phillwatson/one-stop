package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentCancelled;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.task.SendEmailTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ConsentTopicConsumer_ConsentCancelledTest {
    private final SendEmailTask sendEmailTask = mock();
    private final NotificationService notificationService = mock();

    private final ConsentTopicConsumer fixture = new ConsentTopicConsumer(
        sendEmailTask,
        notificationService
    );

    @Test
    public void test() {
        // given: a ConsentCancelled event
        ConsentCancelled event = ConsentCancelled.builder()
            .userId(UUID.randomUUID())
            .consentId(UUID.randomUUID())
            .institutionId(insecure().nextAlphanumeric(30))
            .institutionName(insecure().nextAlphanumeric(30))
            .dateCancelled(Instant.now())
            .agreementId(insecure().nextAlphanumeric(30))
            .agreementExpires(Instant.now().plus(Duration.ofDays(30)))
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailTask).queueTask(eq(event.getUserId()), eq(TemplateName.CONSENT_CANCELLED), paramsCaptor.capture());

        // and: the parameters contain the event payload
        ConsentCancelled param = (ConsentCancelled)paramsCaptor.getValue().get("event");
        assertNotNull(param);
        assertEquals(event.getUserId(), param.getUserId());
        assertEquals(event.getConsentId(), param.getConsentId());

        // and: no notification is issued
        verifyNoInteractions(notificationService);
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.CONSENT, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
