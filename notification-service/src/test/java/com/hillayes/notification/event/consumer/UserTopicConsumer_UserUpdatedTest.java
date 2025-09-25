package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserUpdated;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserTopicConsumer_UserUpdatedTest {
    @Mock
    UserService userService;

    @InjectMocks
    UserTopicConsumer fixture;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @Test
    public void testUserUpdated() {
        // and: a UserUpdated event to update that user
        UserUpdated event = UserUpdated.builder()
            .dateUpdated(Instant.now())
            .userId(UUID.randomUUID())
            .username(insecure().nextAlphanumeric(12))
            .title(insecure().nextAlphanumeric(5))
            .givenName(insecure().nextAlphanumeric(20))
            .familyName(insecure().nextAlphanumeric(20))
            .preferredName(insecure().nextAlphanumeric(20))
            .phoneNumber(insecure().nextNumeric(12))
            .email(insecure().nextAlphanumeric(30))
            .locale(Locale.ENGLISH)
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: the user service is called
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateUser(userCaptor.capture());

        // and: the details match the event
        User update = userCaptor.getValue();
        assertEquals(event.getUserId(), update.getId());
        assertEquals(event.getUsername(), update.getUsername());
        assertEquals(event.getTitle(), update.getTitle());
        assertEquals(event.getGivenName(), update.getGivenName());
        assertEquals(event.getFamilyName(), update.getFamilyName());
        assertEquals(event.getPreferredName(), update.getPreferredName());
        assertEquals(event.getPhoneNumber(), update.getPhoneNumber());
        assertEquals(event.getEmail(), update.getEmail());
        assertEquals(event.getLocale(), update.getLocale());
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
