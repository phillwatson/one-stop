package com.hillayes.shares.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserCreated;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.events.events.user.UserRegistered;
import com.hillayes.events.events.user.UserUpdated;
import com.hillayes.shares.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class UserTopicConsumerTest {
    private final PortfolioRepository portfolioRepository = mock();

    private final UserTopicConsumer userTopicConsumer = new UserTopicConsumer(portfolioRepository);

    @Test
    public void testConsumeUserDeletedEvent() {
        // Given: a UserDeletedEvent with a user ID
        UserDeleted payload = UserDeleted
            .builder()
            .userId(UUID.randomUUID())
            .dateDeleted(Instant.now())
            .build();

        // And: a UserDeleted event with the user identity
        EventPacket eventPacket = new EventPacket(
            UUID.randomUUID(), Topic.USER, UUID.randomUUID().toString(), 0, Instant.now(), null,
            UserDeleted.class.getName(), EventPacket.serialize(payload)
        );

        // When: the consumer accepts the event
        userTopicConsumer.consume(eventPacket);

        // Then: the portfolio repository is called to delete user's portfolios
        verify(portfolioRepository).deleteUsersPortfolios(payload.getUserId());
    }

    @ParameterizedTest
    @MethodSource("provideUserEvents")
    public void testOtherEventTypes(Object payload) {
        // Given: an event package with the given payload
        EventPacket eventPacket = new EventPacket(
            UUID.randomUUID(), Topic.USER, UUID.randomUUID().toString(), 0, Instant.now(), null,
            payload.getClass().getName(), EventPacket.serialize(payload)
        );

        // When: the consumer accepts the event
        userTopicConsumer.consume(eventPacket);

        // Then: the portfolio repository is NOT called
        verifyNoInteractions(portfolioRepository);
    }

    static Stream<Object> provideUserEvents() {
        return Stream.of(
            UserCreated.builder().userId(UUID.randomUUID()).build(),
            UserRegistered.builder().email("").acknowledgerUri(URI.create("http://localhost")).build(),
            UserUpdated.builder().userId(UUID.randomUUID()).build()
        );
    }
}
