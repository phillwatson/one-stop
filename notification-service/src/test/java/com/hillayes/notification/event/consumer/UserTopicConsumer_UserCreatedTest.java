package com.hillayes.notification.event.consumer;

import com.hillayes.notification.domain.User;
import com.hillayes.notification.service.UserService;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserCreated;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class UserTopicConsumer_UserCreatedTest {
    @InjectMock
    UserService userService;

    @Inject
    UserTopicConsumer fixture;

    @Test
    public void testUserCreated() {
        // given: a UserCreated event
        UserCreated event = UserCreated.builder()
            .dateCreated(Instant.now())
            .userId(UUID.randomUUID())
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .email(randomAlphanumeric(30))
            .locale(Locale.ENGLISH)
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: the user details are saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture());

        // and: the details match the event
        User user = userCaptor.getValue();
        assertEquals(event.getDateCreated(), user.getDateCreated());
        assertEquals(event.getUserId(), user.getId());
        assertEquals(event.getUsername(), user.getUsername());
        assertEquals(event.getTitle(), user.getTitle());
        assertEquals(event.getGivenName(), user.getGivenName());
        assertEquals(event.getFamilyName(), user.getFamilyName());
        assertEquals(event.getPreferredName(), user.getPreferredName());
        assertEquals(event.getPhoneNumber(), user.getPhoneNumber());
        assertEquals(event.getEmail(), user.getEmail());
        assertEquals(event.getLocale(), user.getLocale());
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
