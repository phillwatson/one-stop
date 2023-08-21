package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.executors.correlation.Correlation;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A proxy for EventConsumer implementations. This proxy consumer will receive the
 * events for a given collection of topics to which the EventConsumer is listening.
 * Each event will be passed to the EventConsumer (ensuring that the correlation ID
 * is set on the thread). It will handle any errors raised by the EventConsumer by
 * passing them to the given ConsumerErrorHandler.
 *
 * It will also maintain the event queue offsets and respond to changes in the message
 * partition allocations.
 */
@Slf4j
public class ConsumerProxy implements Runnable {
    private static final Duration POLL_TIMEOUT = Duration.ofDays(1);
    private static final int COMMIT_FREQUENCY = 100;

    /**
     * The kafkaConsumer from which events will be polled. This ConsumerProxy will
     * close the connection to this broker on shutdown.
     */
    private final KafkaConsumer<String, EventPacket> broker;

    /**
     * The event consumer to which received events are passed for processing.
     */
    private final EventConsumer eventConsumer;

    /**
     * The topics to which this consumer is subscribed.
     */
    private final Collection<String> topics;

    /**
     * The error handler that will determine if an event should be retried or sent
     * to the message hospital for manual intervention.
     */
    private final ConsumerErrorHandler errorHandler;

    Map<TopicPartition, OffsetAndMetadata> currentOffsets;

    /**
     * The count of events emitted since the last partition assignment for this
     * consumer. This is used to signal when partition offsets are committed, in
     * order to maintain
     */
    int count;

    public ConsumerProxy(KafkaConsumer<String, EventPacket> broker,
                         Collection<Topic> topics,
                         EventConsumer eventConsumer,
                         ConsumerErrorHandler errorHandler) {
        this.broker = broker;
        this.eventConsumer = eventConsumer;
        this.topics = topics.stream().map(Topic::topicName).toList();
        this.errorHandler = errorHandler;
    }

    public void stop() {
        broker.wakeup();
    }

    public Collection<String> getTopics() {
        return topics;
    }

    public void run() {
        log.debug("Starting consumer [topics: {}]", topics);
        try {
            broker.subscribe(topics, new ConsumerRebalanceListener() {
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    log.debug("Partitions assigned [partitions: {}]", partitions);
                    currentOffsets = new HashMap<>();
                    count = 0;
                }

                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    log.trace("Lost partitions in re-balance. Committing current offsets");
                    commitOffsets();
                }
            });

            while (true) {
                log.trace("Polling for events [topics: {}]", topics);
                ConsumerRecords<String, EventPacket> records = broker.poll(POLL_TIMEOUT);
                log.trace("Records polled [topics: {}, size: {}]", topics, records.count());

                for (ConsumerRecord<String, EventPacket> record : records) {
                    if (log.isTraceEnabled()) {
                        log.trace("topic = {}, partition = {}, offset = {}, customer = {}, country = {}",
                            record.topic(), record.partition(), record.offset(), record.key(), record.value());
                    }

                    // using the correlation id from the record
                    String prevId = Correlation.setCorrelationId(record.value().getCorrelationId());
                    try {
                        // pass record to topic handler
                        eventConsumer.consume(record);

                        // commit the offset
                        softCommit(record);
                    } catch (WakeupException e) {
                        throw e;
                    } catch (Exception e) {
                        log.warn("Error from event handler [topic: {}]", record.topic(), e);
                        hardCommit(record);

                        errorHandler.handle(record, e);
                    } finally {
                        Correlation.setCorrelationId(prevId);
                    }
                }
            }
        } catch (WakeupException e) {
            // ignore, we're closing
        } finally {
            try {
                log.info("Closing consumer and committing offsets [topics: {}]", getTopics());
                commitOffsets();
            } finally {
                broker.close();
                log.info("Closed consumer and we are done [topics: {}]", getTopics());
            }
        }
    }

    /**
     * Commits the offset for the given record to the local cache. If the number of
     * records processed since the last commit has reached the commit frequency, the
     * cached offsets are committed to Kafka.
     *
     * @param record the record whose offset is to be committed.
     */
    private void softCommit(ConsumerRecord<?, ?> record) {
        commitRecord(record);
        if (count++ % COMMIT_FREQUENCY == 0) {
            broker.commitAsync(currentOffsets,
                (Map<TopicPartition, OffsetAndMetadata> offsets, Exception error) -> {
                    if (error != null) {
                        log.warn("Failed to commit offsets", error);
                    }
                });
        }
    }

    private void hardCommit(ConsumerRecord<?, ?> record) {
        commitRecord(record);
        commitOffsets();
    }

    private void commitRecord(ConsumerRecord<?, ?> record) {
        currentOffsets.put(
            new TopicPartition(record.topic(), record.partition()),
            new OffsetAndMetadata(record.offset() + 1));
    }

    private void commitOffsets() {
        if (!currentOffsets.isEmpty()) {
            broker.commitSync(currentOffsets);
        }
    }
}
