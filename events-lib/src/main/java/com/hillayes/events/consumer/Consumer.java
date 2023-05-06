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

@Slf4j
public class Consumer implements Runnable {
    private static final Duration POLL_TIMEOUT = Duration.ofDays(1);
    private static final int COMMIT_FREQUENCY = 100;

    private final KafkaConsumer<String, EventPacket> broker;

    /**
     * The event consumer to which received events are passed for processing.
     */
    private final EventConsumer eventConsumer;

    /**
     * The topics to which this consumer is subscribed.
     */
    private final Collection<String> topics;

    private final ConsumerErrorHandler errorHandler;

    Map<TopicPartition, OffsetAndMetadata> currentOffsets;
    int count;

    public Consumer(KafkaConsumer<String, EventPacket> broker,
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
                    log.debug("Lost partitions in re-balance. Committing current offsets");
                    broker.commitSync(currentOffsets);
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
                log.info("Closing consumer and committing offsets");
                if (!currentOffsets.isEmpty()) {
                    broker.commitSync(currentOffsets);
                }
            } finally {
                broker.close();
                log.info("Closed consumer and we are done");
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
        currentOffsets.put(
            new TopicPartition(record.topic(), record.partition()),
            new OffsetAndMetadata(record.offset() + 1));

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
        currentOffsets.put(
            new TopicPartition(record.topic(), record.partition()),
            new OffsetAndMetadata(record.offset() + 1));

        broker.commitSync(currentOffsets);
    }
}
