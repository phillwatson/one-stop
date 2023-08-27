package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static com.hillayes.events.consumer.HeadersUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RetryConsumerTest {
    private EventRepository eventRepository = mock(EventRepository.class);

    private RetryTopicConsumer fixture = new RetryTopicConsumer(eventRepository);

    @Test
    public void testConsume() {
        // given: an EventPacket
        EventPacket eventPacket = createEventPacket();

        // and: a broker message
        ConsumerRecord<String, EventPacket> record = new ConsumerRecord<>(
            "topic", 0, 0, "key", eventPacket
        );

        // and: the message has reason headers - with new scheduled time
        Instant rescheduleAt = Instant.now().plusSeconds(10);
        record.headers()
            .add(REASON_HEADER, "test reason".getBytes())
            .add(CAUSE_HEADER, "test cause".getBytes())
            .add(SCHEDULE_HEADER, rescheduleAt.toString().getBytes());

        // when: the message is consumed
        fixture.consume(record);

        // then: the event is persisted
        ArgumentCaptor<EventEntity> argument = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).persist(argument.capture());

        // and: the event entity is created
        EventEntity eventEntity = argument.getValue();
        assertEquals(eventPacket.getId(), eventEntity.getEventId());
        assertEquals(eventPacket.getCorrelationId(), eventEntity.getCorrelationId());
        assertEquals(eventPacket.getKey(), eventEntity.getKey());
        assertEquals(eventPacket.getTopic(), eventEntity.getTopic());
        assertEquals(eventPacket.getPayloadClass(), eventEntity.getPayloadClass());
        assertEquals(eventPacket.getPayload(), eventEntity.getPayload());

        // and: the events retry count is incremented
        assertEquals(eventPacket.getRetryCount() + 1, eventEntity.getRetryCount());

        // and: the event scheduled time is set to the value in message header
        assertEquals(rescheduleAt, eventEntity.getScheduledFor());
    }

    @Test
    public void testConsume_NoScheduleGiven() {
        // given: an EventPacket
        EventPacket eventPacket = createEventPacket();

        // and: a broker message
        ConsumerRecord<String, EventPacket> record = new ConsumerRecord<>(
            "topic", 0, 0, "key", eventPacket
        );

        // and: the message has reason headers - with NO scheduled time
        record.headers()
            .add(REASON_HEADER, "test reason".getBytes())
            .add(CAUSE_HEADER, "test cause".getBytes());

        // when: the message is consumed
        fixture.consume(record);

        // then: the event is persisted
        ArgumentCaptor<EventEntity> argument = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).persist(argument.capture());

        // and: the event entity is created
        EventEntity eventEntity = argument.getValue();
        assertEquals(eventPacket.getId(), eventEntity.getEventId());
        assertEquals(eventPacket.getCorrelationId(), eventEntity.getCorrelationId());
        assertEquals(eventPacket.getKey(), eventEntity.getKey());
        assertEquals(eventPacket.getTopic(), eventEntity.getTopic());
        assertEquals(eventPacket.getPayloadClass(), eventEntity.getPayloadClass());
        assertEquals(eventPacket.getPayload(), eventEntity.getPayload());

        // and: the events retry count is incremented
        assertEquals(eventPacket.getRetryCount() + 1, eventEntity.getRetryCount());

        // and: the event scheduled time is set to some future time
        assertTrue(eventEntity.getScheduledFor().isAfter(Instant.now()));
        assertTrue(eventEntity.getScheduledFor().isBefore(Instant.now().plus(RetryTopicConsumer.DEFAULT_RESCHEDULE_OFFSET)));
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
