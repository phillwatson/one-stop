package com.hillayes.events.sender;

import com.hillayes.events.domain.EventPacket;
import io.smallrye.common.annotation.Identifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
@Slf4j
public class ProducerFactory {
    private final Properties producerConfig;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private volatile Producer<String, EventPacket> producer;

    public ProducerFactory(@Identifier("event-producer-config") Properties producerConfig) {
        this.producerConfig = producerConfig;
    }

    public Producer<String, EventPacket> getProducer() {
        if (shutdown.get()) {
            throw new RuntimeException("Application shutting down. Unable to issue Message Producer");
        }

        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    log.info("Creating new event Kafka Producer");
                    producer = new KafkaProducer<>(producerConfig);
                }
            }
        }
        return producer;
    }

    @PreDestroy
    public void onStop() {
        log.info("Shutting down Message Producers - started");
        shutdown.set(true);
        if (producer != null) {
            log.info("Closing producer {}", producer);
            producer.close();
        }
        log.info("Shutting down Message Producers - complete");
    }
}
