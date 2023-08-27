package com.hillayes.audit.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@QuarkusTest
public class UserAuthConsumerTest {
    private final ObjectMapper objectMapper = MapperFactory.defaultMapper();

    @Inject
    UserAuthTopicConsumer userAuthTopicConsumer;

    @Test
    public void testConsume() throws Exception {
        UserAuthenticated payload = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        EventPacket eventPacket = new EventPacket(UUID.randomUUID(), Topic.USER_AUTH, UUID.randomUUID().toString(),
        0, Instant.now(), null, UserAuthenticated.class.getName(), objectMapper.writeValueAsString(payload));

        userAuthTopicConsumer.consume(eventPacket);
    }
}
