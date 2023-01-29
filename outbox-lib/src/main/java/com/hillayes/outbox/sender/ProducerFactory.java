package com.hillayes.outbox.sender;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.outbox.config.ProducerConfig;
import io.quarkus.runtime.ShutdownEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
@Slf4j
public class ProducerFactory {
    private final Properties producerConfig;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private volatile Producer<String, EventPacket> producer;

    public ProducerFactory(@ProducerConfig Properties producerConfig) {
        this.producerConfig = producerConfig;
    }

    public Producer<String, EventPacket> getProducer() {
        if (shutdown.get()) {
            throw new RuntimeException("Application shutting down. Unable to issue Message Producer");
        }

        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    producer = new KafkaProducer<>(producerConfig);
                }
            }
        }
        return producer;
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Shutting down Message Producers - started");
        shutdown.set(true);
        if (producer != null) {
            log.info("Closing producer {}", producer);
            producer.close();
        }
        log.info("Shutting down Message Producers - complete");
    }
}
