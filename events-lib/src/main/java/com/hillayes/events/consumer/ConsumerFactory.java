package com.hillayes.events.consumer;

import com.hillayes.events.annotation.AnnotationUtils;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.serializers.EventPacketDeserializer;
import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import java.util.*;
import java.util.concurrent.ExecutorService;

@ApplicationScoped
//@UnlessBuildProfile("test")
@Slf4j
public class ConsumerFactory {
    private final Set<Consumer> consumers = new HashSet<>();

    public void startConsumers(@Observes StartupEvent ev,
                               Set<Consumer> consumers) {
        if (consumers.isEmpty()) {
            log.warn("No consumers registered");
            return;
        }

        ExecutorService executorService = ExecutorFactory.newExecutor(ExecutorConfiguration.builder()
            .name("topic-consumer")
            .executorType(ExecutorType.FIXED)
            .numberOfThreads(consumers.size())
            .build());

        consumers.forEach(consumer -> {
            log.debug("Starting consumer [topics: {}]", consumer.getTopics());
            executorService.submit(consumer);
        });
    }

    @PreDestroy
    public void shutdownConsumers() {
        log.info("Shutting down consumers");
        consumers.forEach(consumer -> {
            log.debug("Shutting down consumer [topics: {}]", consumer.getTopics());
            consumer.stop();
        });
    }

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

    @Produces
    @ApplicationScoped
    public Set<Consumer> consumers(@Any Instance<EventConsumer> instances,
                                   @Identifier("event-consumer-config") Properties consumerConfig,
                                   ConsumerErrorHandler errorHandler) {
        log.info("Registering consumers");
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

                consumers.add(new Consumer(new KafkaConsumer<>(config), topics, eventConsumer, errorHandler));
            }
        });

        return consumers;
    }
}
