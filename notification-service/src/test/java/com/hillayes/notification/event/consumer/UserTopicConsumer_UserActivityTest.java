package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.AccountActivity;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.notification.config.TemplateName;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class UserTopicConsumer_UserActivityTest {
    @InjectMock
    SendEmailTask sendEmailTask;

    @Inject
    UserTopicConsumer fixture;

    @Test
    public void testUserActivity() {
        // given: a UserRegistered event
        AccountActivity event = AccountActivity.builder()
            .userId(UUID.randomUUID())
            .activity(SuspiciousActivity.EMAIL_REGISTRATION)
            .dateRecorded(Instant.now())
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailTask).queueJob(eq(event.getUserId()), eq(TemplateName.ACCOUNT_ACTIVITY), paramsCaptor.capture());

        // and: the email template parameters are taken from the event payload
        assertEquals(event.getActivity().getMessage(), paramsCaptor.getValue().get("activity"));
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
