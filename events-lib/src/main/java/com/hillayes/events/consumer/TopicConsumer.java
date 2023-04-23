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

    public void run(String topic) {
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
        } catch (WakeupException e) {
            // ignore, we're closing
        } catch (Exception e) {
            log.error("Unexpected error", e);
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
}
