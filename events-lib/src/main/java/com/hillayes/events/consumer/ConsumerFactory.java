package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.serializers.EventPacketDeserializer;
import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import io.quarkus.runtime.Startup;
import io.smallrye.common.annotation.Identifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import java.util.*;
import java.util.concurrent.ExecutorService;

@ApplicationScoped
//@UnlessBuildProfile("test")
@Slf4j
public class ConsumerFactory {
    private final Set<TopicConsumer> consumers = new HashSet<>();

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

    @Produces
    @ApplicationScoped
    public ConsumerErrorHandler errorHandler(Producer<String, EventPacket> producer) {
        return new ConsumerErrorHandler(producer);
    }

    @Produces
    @ApplicationScoped
    @Startup
    public Set<TopicConsumer> consumers(@Any Instance<EventConsumer> instances,
                                        @Identifier("event-consumer-config") Properties consumerConfig,
                                        ConsumerErrorHandler errorHandler) {
        log.info("Registering consumers");
        instances.stream().forEach(eventConsumer -> {
            Collection<Topic> topics = getTopics(eventConsumer);
            if (!topics.isEmpty()) {
                log.debug("Registering consumer [topics: {}, class: {}]", topics, eventConsumer.getClass().getName());
                consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, eventConsumer.getClass().getSimpleName());

                TopicConsumer topicConsumer = new TopicConsumer(consumerConfig, topics, eventConsumer, errorHandler);
                consumers.add(topicConsumer);
            }
        });

        if (consumers.isEmpty()) {
            log.warn("No consumers registered");
        } else {
            ExecutorService executorService = ExecutorFactory.newExecutor(ExecutorConfiguration.builder()
                .name("topic-consumer")
                .executorType(ExecutorType.FIXED)
                .numberOfThreads(consumers.size())
                .build());

            consumers.forEach(topicConsumer -> {
                log.debug("Starting consumer [topics: {}]", topicConsumer.getTopics());
                executorService.submit(topicConsumer);
            });
        }

        return consumers;
    }

    @PreDestroy
    public void shutdownConsumers() {
        log.info("Shutting down consumers");
        consumers.forEach(topicConsumer -> {
            log.debug("Shutting down consumer [topics: {}]", topicConsumer.getTopics());
            topicConsumer.stop();
        });
    }

    private Collection<Topic> getTopics(Object instance) {
        return getTopics(new HashSet<>(), instance.getClass());
    }

    private Collection<Topic> getTopics(Collection<Topic> topics, Class<?> clazz) {
        if (clazz != null) {
            for (ConsumerTopic topic : clazz.getAnnotationsByType(ConsumerTopic.class))
                topics.add(topic.topic());
            getTopics(topics, clazz.getSuperclass());
        }
        return topics;
    }
}
