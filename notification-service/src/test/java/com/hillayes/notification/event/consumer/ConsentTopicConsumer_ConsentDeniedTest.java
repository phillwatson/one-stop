package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentDenied;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.task.SendEmailTask;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class ConsentTopicConsumer_ConsentDeniedTest {
    @InjectMock
    SendEmailTask sendEmailTask;

    @InjectMock
    NotificationService notificationService;

    @Inject
    ConsentTopicConsumer fixture;

    @Test
    public void test() {
        // given: a ConsentDenied event
        ConsentDenied event = ConsentDenied.builder()
            .userId(UUID.randomUUID())
            .consentId(UUID.randomUUID())
            .institutionId(randomAlphanumeric(30))
            .institutionName(randomAlphanumeric(30))
            .dateDenied(Instant.now())
            .agreementId(randomAlphanumeric(30))
            .requisitionId(randomAlphanumeric(30))
            .agreementExpires(Instant.now().plus(Duration.ofDays(30)))
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailTask).queueJob(eq(event.getUserId()), eq(TemplateName.CONSENT_DENIED), paramsCaptor.capture());

        // and: the parameters contain the event payload
        ConsentDenied param = (ConsentDenied)paramsCaptor.getValue().get("event");
        assertNotNull(param);
        assertEquals(event.getUserId(), param.getUserId());
        assertEquals(event.getConsentId(), param.getConsentId());

        // and: a notification is issued
        verify(notificationService).createNotification(eq(event.getUserId()), any(),
            eq(NotificationId.CONSENT_DENIED), paramsCaptor.capture());

        // and: the parameters contain the event payload
        param = (ConsentDenied)paramsCaptor.getValue().get("event");
        assertNotNull(param);
        assertEquals(event.getUserId(), param.getUserId());
        assertEquals(event.getConsentId(), param.getConsentId());
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.CONSENT, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
