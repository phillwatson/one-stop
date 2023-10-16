package com.hillayes.notification.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.notification.repository.UserRepository;
import com.hillayes.notification.service.UserService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserTopicConsumer_UserDeletedTest {
    @InjectMock
    UserService userService;

    @InjectMock
    UserRepository userRepository;

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

    @Test
    public void testUserDeleted_UserNotFound() {
        // given: NO existing user record
        when(userRepository.findByIdOptional(any())).thenReturn(Optional.empty());

        // and: a UserDeleted event to update that user
        UserDeleted event = UserDeleted.builder()
            .dateDeleted(Instant.now())
            .userId(UUID.randomUUID())
            .build();
        EventPacket eventPacket = mockEventPacket(event);

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: the user service is called
        verify(userService).deleteUser(event.getUserId());

        // and: NO user record is deleted
        verify(userRepository, never()).delete(any());
    }

    private EventPacket mockEventPacket(Object payload) {
        return new EventPacket(UUID.randomUUID(),
            Topic.USER, UUID.randomUUID().toString(),
            0, Instant.now(),
            null, payload.getClass().getName(), EventPacket.serialize(payload));
    }
}
