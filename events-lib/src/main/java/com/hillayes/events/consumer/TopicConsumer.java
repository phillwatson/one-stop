package com.hillayes.events.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TopicConsumer<K, V> {
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(2);
    private static final int COMMIT_FREQUENCY = 250;

    private KafkaConsumer<K, V> consumer;

    Map<TopicPartition, OffsetAndMetadata> currentOffsets;
    int count;

    public void stop() {
        consumer.wakeup();
    }

    public void run(String topic, EventConsumer<V, ?> eventConsumer) {
        consumer.subscribe(List.of(topic), new ConsumerRebalanceListener() {
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                currentOffsets = new HashMap<>();
                count = 0;
            }

            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.debug("Lost partitions in re-balance. Committing current offsets");
                consumer.commitSync(currentOffsets);
            }
        });

        try {
            while (true) {
                ConsumerRecords<K, V> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<K, V> record : records) {
                    log.debug("topic = {}, partition = {}, offset = {}, customer = {}, country = {}",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());

                    try {
                        // pass record to topic handler
                        eventConsumer.consume(record.value());

                        // commit the offset
                        softCommit(record);
                    } catch (WakeupException e) {
                        throw e;
                    } catch (Exception e) {
                        log.warn("Error from event handler [topic: {}]", record.topic(), e);
                        consumer.commitSync(currentOffsets);

                        // call exception handler - to perform retry or dead-letter
                    }
                }
            }
        } catch (WakeupException e) {
            // ignore, we're closing
        } finally {
            try {
                log.info("Closing consumer and committing offsets");
                consumer.commitSync(currentOffsets);
            } finally {
                consumer.close();
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
    private void softCommit(ConsumerRecord<K, V> record) {
        currentOffsets.put(
            new TopicPartition(record.topic(), record.partition()),
            new OffsetAndMetadata(record.offset() + 1));

        if (count % COMMIT_FREQUENCY == 0) {
            consumer.commitAsync(currentOffsets,
                (Map<TopicPartition, OffsetAndMetadata> offsets, Exception error) -> {
                    if (error != null) {
                        log.warn("Failed to commit offsets", error);
                    }
                });
        }
        count++;
    }
}
