package com.hillayes.events.sender;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.serializers.EventPacketSerializer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.annotation.Identifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Properties;

@RegisterForReflection(targets = {
    // workaround: see https://github.com/quarkusio/quarkus/issues/34071
    // https://quarkus.io/guides/writing-native-applications-tips#registering-for-reflection
    org.apache.kafka.common.serialization.StringSerializer.class,
    com.hillayes.events.serializers.EventPacketSerializer.class,
    org.apache.kafka.common.metrics.JmxReporter.class
})
@Slf4j
public class ProducerFactory {
    @Produces
    @ApplicationScoped
    @Identifier("event-producer-config")
    public Properties getProducerConfig(@ConfigProperty(name = "kafka.producer.client", defaultValue = "hillayes.com") final String clientId,
                                        @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "kafka:9092") final String bootstrapServers,
                                        @ConfigProperty(name = "kafka.producer.lingerMs", defaultValue = "0") final String lingerMs,
                                        @ConfigProperty(name = "kafka.producer.acksConfig", defaultValue = "all") final String acksConfig,
                                        @ConfigProperty(name = "kafka.producer.maxInFlightRequestsPerConnection", defaultValue = "3") final Integer maxInFlightRequestsPerConnection,
                                        @ConfigProperty(name = "kafka.producer.batchSizeConfig", defaultValue = "16384") final Integer batchSizeConfig,
                                        @ConfigProperty(name = "kafka.producer.maxBlockMsConfig", defaultValue = "60000") final Integer maxBlockMsConfig) {
        Properties config = new Properties();
        config.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, acksConfig);
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventPacketSerializer.class);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSizeConfig);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMsConfig);

        log.debug("Kafka producer config: {}", config);
        return config;
    }

    @Produces()
    @ApplicationScoped
    public Producer<String, EventPacket> getProducer(@Identifier("event-producer-config") Properties producerConfig) {
        log.info("Creating new Kafka Producer");
        return new KafkaProducer<>(producerConfig);
    }
}
