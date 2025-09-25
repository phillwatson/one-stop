package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentTimedOut;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.task.SendEmailTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

public class ConsentTopicConsumer_ConsentTimedOutTest {
    @Mock
    SendEmailTask sendEmailTask;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    ConsentTopicConsumer fixture;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @Test
    public void test() {
        // given: a ConsentTimedOut event
        ConsentTimedOut event = ConsentTimedOut.builder()
            .userId(UUID.randomUUID())
            .consentId(UUID.randomUUID())
            .institutionId(insecure().nextAlphanumeric(30))
            .institutionName(insecure().nextAlphanumeric(30))
            .dateTimeout(Instant.now())
            .agreementId(insecure().nextAlphanumeric(30))
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: a notification is issued
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).createNotification(eq(event.getUserId()), any(),
            eq(NotificationId.CONSENT_TIMEOUT), paramsCaptor.capture());

        // and: the parameters contain the event payload
        ConsentTimedOut param = (ConsentTimedOut) paramsCaptor.getValue().get("event");
        assertNotNull(param);
        assertEquals(event.getUserId(), param.getUserId());
        assertEquals(event.getConsentId(), param.getConsentId());

        // and: the parameters contain the event payload
        param = (ConsentTimedOut)paramsCaptor.getValue().get("event");
        assertNotNull(param);
        assertEquals(event.getUserId(), param.getUserId());
        assertEquals(event.getConsentId(), param.getConsentId());

        // and: NO email is sent to the user
        verifyNoInteractions(sendEmailTask);
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.CONSENT, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
