package com.hillayes.executors.scheduler;

import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import com.hillayes.executors.scheduler.helpers.FrequencyConfigImpl;
import com.hillayes.executors.scheduler.helpers.NamedTaskConfigImpl;
import com.hillayes.executors.scheduler.helpers.TestBase;
import com.hillayes.executors.scheduler.tasks.NamedScheduledTask;
import com.hillayes.executors.scheduler.tasks.NamedTask;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ScheduledTaskTest extends TestBase {
    @Test
    public void testDailyTask() {
        AtomicBoolean signal = new AtomicBoolean();

        NamedTask task = new NamedScheduledTask() {
            public String getName() { return "test-daily"; }
            public void run() {
                log.info("Running task ({})", signal.getAndSet(true));
            }
        };

        LocalTime timeOfDay = LocalTime.now().plusSeconds(5);
        Map<String, NamedTaskConfig> taskConfigs = Map.of(
            task.getName(), NamedTaskConfigImpl.builder()
                .frequency(FrequencyConfigImpl.builder()
                    .timeOfDay(timeOfDay.toString())
                    .build())
                .build()
        );

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(), mockConfiguration(taskConfigs),
            List.of(task));

        Awaitility.await()
            .pollInterval(5, TimeUnit.SECONDS)
            .atMost(20, TimeUnit.SECONDS)
            .until(signal::get);

        fixture.stop();
    }

    @Test
    public void testFixedDelay() {
        AtomicInteger signal = new AtomicInteger();

        NamedTask task = new NamedScheduledTask() {
            public String getName() { return "test-fixed-delay"; }
            public void run() {
                log.info("Running task ({})", signal.incrementAndGet());
            }
        };

        Map<String, NamedTaskConfig> taskConfigs = Map.of(
            task.getName(), NamedTaskConfigImpl.builder()
                .frequency(FrequencyConfigImpl.builder()
                    .recurs(Duration.ofSeconds(5))
                    .build())
                .build()
        );

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(), mockConfiguration(taskConfigs),
            List.of(task));

        Awaitility.await()
            .pollInterval(3, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> signal.get() == 3);

        fixture.stop();
    }
}
