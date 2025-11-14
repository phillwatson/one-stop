package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.service.UserService;
import com.hillayes.notification.task.SendEmailTask;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserTopicConsumer_UserDeletedTest {
    private final UserService userService = mock();
    private final SendEmailTask sendEmailTask = mock();
    private final NotificationService notificationService = mock();

    private final UserTopicConsumer fixture = new UserTopicConsumer(
        userService,
        sendEmailTask,
        notificationService
    );

    @Test
    public void testUserDeleted() {
        // given: a UserDeleted event to update that user
        UserDeleted event = UserDeleted.builder()
            .dateDeleted(Instant.now())
            .userId(UUID.randomUUID())
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: the user service is called
        verify(userService).deleteUser(event.getUserId());
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
