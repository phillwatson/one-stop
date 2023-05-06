package com.hillayes.audit.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.events.consumer.ConsumerFactory;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserLogin;
import com.hillayes.events.serializers.MapperFactory;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@QuarkusTest
public class UserAuthConsumerTest {
    private ObjectMapper objectMapper = MapperFactory.defaultMapper();

    @Inject
    UserAuthTopicConsumer userAuthTopicConsumer;

    @Test
    public void testConsume() throws Exception {
        UserLogin payload = UserLogin.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        EventPacket eventPacket = new EventPacket(UUID.randomUUID(), Topic.USER_AUTH, UUID.randomUUID().toString(),
        0, Instant.now(), null, UserLogin.class.getName(), objectMapper.writeValueAsString(payload));

        userAuthTopicConsumer.consume(eventPacket);
    }
}
