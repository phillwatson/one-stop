package com.hillayes.events.consumer;

import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutorService;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
//@UnlessBuildProfile("test")
public class ConsumerStartup {
    private final Set<ConsumerProxy> consumers;

    public void startConsumers(@Observes StartupEvent ev) {
        log.info("Starting event consumers");
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
        log.info("Shutting down event consumers");
        consumers.forEach(consumer -> {
            log.debug("Shutting down consumer [topics: {}]", consumer.getTopics());
            consumer.stop();
        });
    }
}
