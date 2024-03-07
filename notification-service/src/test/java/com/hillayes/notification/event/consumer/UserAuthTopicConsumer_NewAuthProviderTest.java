package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.NewAuthProvider;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.task.SendEmailTask;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
public class UserAuthTopicConsumer_NewAuthProviderTest {
    @InjectMock
    SendEmailTask sendEmailTask;

    @InjectMock
    NotificationService notificationService;

    @Inject
    UserAuthTopicConsumer fixture;

    @Test
    public void testUserActivity() {
        // given: a UserRegistered event
        NewAuthProvider event = NewAuthProvider.builder()
            .userId(UUID.randomUUID())
            .authProvider("google")
            .dateLogin(Instant.now())
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailTask).queueJob(eq(event.getUserId()), eq(TemplateName.NEW_OIDC_LOGIN), paramsCaptor.capture());

        // and: the email template parameters are taken from the event payload
        // and: the parameters contain the event payload
        NewAuthProvider param = (NewAuthProvider)paramsCaptor.getValue().get("event");
        assertNotNull(param);
        assertEquals(event.getUserId(), param.getUserId());
        assertEquals(event.getAuthProvider(), param.getAuthProvider());

        // and: no notification is issued
        verifyNoInteractions(notificationService);
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
