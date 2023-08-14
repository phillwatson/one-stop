package com.hillayes.events.consumer;

import com.hillayes.events.annotation.AnnotationUtils;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.serializers.EventPacketDeserializer;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

@Slf4j
public class ConsumerFactory {
    @Produces
    @ApplicationScoped
    @Identifier("event-consumer-config")
    public Properties consumerConfig(@ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "kafka:9092") String bootstrapServers,
                                     @ConfigProperty(name = "kafka.group.id") Optional<String> groupId,
                                     @ConfigProperty(name = "quarkus.application.name") String applicationName) {
        Properties config = new Properties();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId.orElse(applicationName));
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventPacketDeserializer.class);

        log.debug("Kafka consumer config: {}", config);
        return config;
    }

    /**
     * Creates the error handler that will decide if an event is to be retried or
     * placed on the message-hospital queue.
     *
     * @param producer the producer used to requeue events. See ProducerFactory.
     * @return the configured ConsumerErrorHandler instance.
     */
    @Produces
    @ApplicationScoped
    public ConsumerErrorHandler errorHandler(Producer<String, EventPacket> producer) {
        return new ConsumerErrorHandler(producer);
    }

    /**
     * Creates a proxy for each EventConsumer instance located by the CDI context.
     *
     * @param instances the EventConsumer instances found in the CDI context.
     * @param consumerConfig the broker consumer configuration.
     * @param errorHandler the error handler to which event errors are passed during
     * consumption.
     * @return the collection of ConsumerProxies.
     */
    @Produces
    @ApplicationScoped
    public Set<ConsumerProxy> consumers(@Any Instance<EventConsumer> instances,
                                        @Identifier("event-consumer-config") Properties consumerConfig,
                                        ConsumerErrorHandler errorHandler) {
        log.info("Registering consumers");
        Set<ConsumerProxy> consumers = new HashSet<>();
        instances.stream().forEach(eventConsumer -> {
            Collection<Topic> topics = AnnotationUtils.getTopics(eventConsumer);
            Optional<String> consumerGroup = AnnotationUtils.getConsumerGroup(eventConsumer);
            if (!topics.isEmpty()) {
                log.debug("Registering consumer [topics: {}, class: {}]", topics, eventConsumer.getClass().getName());

                // Create a new config for each consumer - with defaults supplied from the global config
                Properties config = new Properties();
                config.putAll(consumerConfig);
                config.put(ConsumerConfig.CLIENT_ID_CONFIG, eventConsumer.getClass().getSimpleName());
                consumerGroup.ifPresent(group -> config.put(ConsumerConfig.GROUP_ID_CONFIG, group));

                consumers.add(new ConsumerProxy(new KafkaConsumer<>(config), topics, eventConsumer, errorHandler));
            }
        });

        return consumers;
    }
}
