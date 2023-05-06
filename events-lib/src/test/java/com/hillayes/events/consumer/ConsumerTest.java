package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ConsumerTest {
    KafkaConsumer<String, EventPacket> broker;
    EventConsumer eventConsumer;
    ConsumerErrorHandler errorHandler;

    ConsumerRecords<String, EventPacket> events = ConsumerRecords.empty();

    Supplier<ConsumerRecords<String, EventPacket>> records = () -> events;

    @BeforeEach
    public void beforeEach() {
        broker = mockBroker(records);
        eventConsumer = mock(EventConsumer.class);
        errorHandler = mock(ConsumerErrorHandler.class);
    }

    @Test
    public void testGetTopics() {
        // given: a consumer with two topics
        Consumer fixture = new Consumer(broker, List.of(Topic.USER_AUTH, Topic.USER), eventConsumer, errorHandler);

        // when: the consumer's topics are retrieved
        Collection<String> topics = fixture.getTopics();

        // then: the topics are returned
        assertEquals(2, topics.size());
        assertTrue(topics.contains(Topic.USER_AUTH.topicName()));
        assertTrue(topics.contains(Topic.USER.topicName()));
    }

    @Test
    public void testRun() {
        // given: a consumer
        Consumer fixture = new Consumer(broker, List.of(Topic.USER_AUTH), eventConsumer, errorHandler);

        // when: the consumer is run
        new Thread(fixture).start();

        // then: the consumer is subscribed to the topic
        Awaitility.await().untilAsserted(() -> verify(broker).subscribe(eq(List.of(Topic.USER_AUTH.topicName())), any()));

        // and: the consumer polls for records
        Awaitility.await().untilAsserted(() -> verify(broker, atLeastOnce()).poll(any()));
    }

    @Test
    public void testStop() {
        // given: a consumer
        Consumer fixture = new Consumer(broker, List.of(Topic.USER_AUTH), eventConsumer, errorHandler);

        // and: the consumer is run
        new Thread(fixture).start();

        // and: the consumer polls for records
        Awaitility.await().untilAsserted(() -> verify(broker, atLeastOnce()).poll(any()));

        // when: the consumer is stopped
        fixture.stop();

        // then: the consumer is woken up
        verify(broker).wakeup();

        // and: the broker is closed
        Awaitility.await().untilAsserted(() -> verify(broker).close());
    }

    @Test
    public void testConsumerRecords() {
        // given: a consumer
        Consumer fixture = new Consumer(broker, List.of(Topic.USER_AUTH), eventConsumer, errorHandler);

        // and: the consumer is run
        new Thread(fixture).start();

        // and: the consumer polls for records
        Awaitility.await().untilAsserted(() -> verify(broker, atLeastOnce()).poll(any()));

        // when: the records are provided
        int size = postEvents(List.of(
            new ConsumerRecord<>(Topic.USER_AUTH.topicName(), 0, 0, "key", mock(EventPacket.class)),
            new ConsumerRecord<>(Topic.USER_AUTH.topicName(), 0, 1, "key", mock(EventPacket.class))
        ));

        // then: the consumer processes the records
        Awaitility.await().untilAsserted(() ->
            verify(eventConsumer, times(size)).consume(any(ConsumerRecord.class))
        );

        // when: the consumer is stopped
        fixture.stop();

        // then: the consumer is woken up
        verify(broker).wakeup();

        // and: the broker is closed
        Awaitility.await().untilAsserted(() -> verify(broker).close());

        // and: the event offsets are committed
        verify(broker).commitSync(any(Map.class));

        // and: the error handler is never called
        verify(errorHandler, never()).handle(any(ConsumerRecord.class), any(RuntimeException.class));
    }

    @Test
    public void testErrorHandler() throws Exception {
        // given: a consumer
        Consumer fixture = new Consumer(broker, List.of(Topic.USER_AUTH), eventConsumer, errorHandler);

        // and: a faulty event consumer
        doThrow(new RuntimeException("test")).when(eventConsumer).consume(any(ConsumerRecord.class));

        // and: the consumer is run
        new Thread(fixture).start();

        // and: the consumer polls for records
        Awaitility.await().untilAsserted(() -> verify(broker, atLeastOnce()).poll(any()));

        // when: the records are provided
        int size = postEvents(List.of(
            new ConsumerRecord<>(Topic.USER_AUTH.topicName(), 0, 0, "key", mock(EventPacket.class)),
            new ConsumerRecord<>(Topic.USER_AUTH.topicName(), 0, 1, "key", mock(EventPacket.class))
        ));

        // and: the consumer processes the records
        Awaitility.await().untilAsserted(() ->
            verify(eventConsumer, times(size)).consume(any(ConsumerRecord.class))
        );

        // then: the error handler is called
        verify(errorHandler, times(size)).handle(any(ConsumerRecord.class), any(RuntimeException.class));

        // and: the event offsets are committed
        verify(broker, times(size)).commitSync(any(Map.class));
    }

    private int postEvents(List<ConsumerRecord<String, EventPacket>> eventList) {
        events = new ConsumerRecords<>(Map.of(
            new TopicPartition(Topic.USER_AUTH.topicName(), 0), eventList
        ));
        return eventList.size();
    }

    private KafkaConsumer<String, EventPacket> mockBroker(Supplier<ConsumerRecords<String, EventPacket>> records) {
        KafkaConsumer<String, EventPacket> result = mock(KafkaConsumer.class);

        final AtomicBoolean stopped = new AtomicBoolean(false);
        doAnswer(invocation -> {
            stopped.set(true);
            return null;
        }).when(result).wakeup();

        when(result.poll(any())).thenAnswer(invocation -> {
            Thread.sleep(500);
            if (stopped.get())
                throw new WakeupException();
            return records.get();
        });

        doAnswer(invocation -> {
            ConsumerRebalanceListener listener = invocation.getArgument(1);
            listener.onPartitionsAssigned(List.of(new TopicPartition(Topic.USER_AUTH.topicName(), 0)));
            return null;
        }).when(result).subscribe(any(Collection.class), any(ConsumerRebalanceListener.class));

        return result;
    }
}
