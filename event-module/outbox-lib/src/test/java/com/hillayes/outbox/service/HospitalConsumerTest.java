package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import com.hillayes.outbox.repository.HospitalEntity;
import com.hillayes.outbox.repository.HospitalRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static com.hillayes.events.consumer.HeadersUtils.CAUSE_HEADER;
import static com.hillayes.events.consumer.HeadersUtils.REASON_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HospitalConsumerTest {
    private HospitalRepository hospitalRepository = mock(HospitalRepository.class);

    private HospitalTopicConsumer fixture = new HospitalTopicConsumer(hospitalRepository);

    @Test
    public void testConsume() {
        // given: an EventPacket
        EventPacket eventPacket = createEventPacket();

        // and: a broker message
        ConsumerRecord<String, EventPacket> record = new ConsumerRecord<>(
            "topic", 0, 0, "key", eventPacket
        );
        record.headers()
            .add(REASON_HEADER, "test reason".getBytes())
            .add(CAUSE_HEADER, "test cause".getBytes());

        // when: the message is consumed
        fixture.consume(record);

        // then: the event is persisted
        ArgumentCaptor<HospitalEntity> argument = ArgumentCaptor.forClass(HospitalEntity.class);
        verify(hospitalRepository).save(argument.capture());

        // and: the hospital entity is created
        HospitalEntity hospitalEntity = argument.getValue();
        assertEquals(eventPacket.getId(), hospitalEntity.getEventId());
        assertEquals(eventPacket.getCorrelationId(), hospitalEntity.getCorrelationId());
        assertEquals(eventPacket.getKey(), hospitalEntity.getKey());
        assertEquals(eventPacket.getTopic(), hospitalEntity.getTopic());
        assertEquals(eventPacket.getPayloadClass(), hospitalEntity.getPayloadClass());
        assertEquals(eventPacket.getPayload(), hospitalEntity.getPayload());
        assertEquals(eventPacket.getRetryCount(), hospitalEntity.getRetryCount());
        assertEquals("test reason", hospitalEntity.getReason());
        assertEquals("test cause", hospitalEntity.getCause());
    }

    private EventPacket createEventPacket() {
        UserAuthenticated payload = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        return new EventPacket(
            UUID.randomUUID(),
            Topic.USER_AUTH,
            UUID.randomUUID().toString(),
            0, Instant.now(),
            "key", payload.getClass().getName(), EventPacket.serialize(payload)
        );
    }
}
