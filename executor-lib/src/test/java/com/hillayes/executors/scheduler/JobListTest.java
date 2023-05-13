package com.hillayes.executors.scheduler;

import com.hillayes.executors.scheduler.helpers.NamedTaskConfigImpl;
import com.hillayes.executors.scheduler.helpers.SchedulerConfigImpl;
import com.hillayes.executors.scheduler.helpers.TestBase;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JobListTest extends TestBase {
    @Test
    public void testJobList() {
        final AtomicInteger signal = new AtomicInteger();

        NamedJobbingTask<String> task = new NamedJobbingTask<>() {
            public String getName() {
                return "test-jobs";
            }

            public void accept(String data) {
                log.info("Task {} is running ({})", data, signal.incrementAndGet());
            }

            @Override
            public String queueJob(String payload) {
                return null;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Collections.emptyMap())
                .build(), List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");
        fixture.addJob(task, "two");
        fixture.addJob(task, "three");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(5, TimeUnit.SECONDS)
            .atMost(20, TimeUnit.SECONDS)
            .until(() -> signal.get() == 3);

        // queue some more jobs
        fixture.addJob(task, "four");
        fixture.addJob(task, "five");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(2, TimeUnit.SECONDS)
            .atMost(20, TimeUnit.SECONDS)
            .until(() -> signal.get() == 5);

        fixture.stop();
    }

    @Test
    public void testRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedJobbingTask<String> task = new NamedJobbingTask<>() {
            public String getName() {
                return "test-jobs";
            }

            public void accept(String data) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running ({})", data, count);
                if (count < 3) {
                    throw new RuntimeException("test task failure");
                }
            }

            @Override
            public String queueJob(String payload) {
                return null;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .retryInterval(Duration.ofSeconds(1))
                    .retryExponent(1.0)
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(2, TimeUnit.SECONDS)
            .atMost(25, TimeUnit.SECONDS)
            .until(() -> signal.get() == 3);
        fixture.stop();
    }

    @Test
    public void testMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedJobbingTask<String> task = new NamedJobbingTask<>() {
            public String getName() {
                return "test-jobs";
            }

            // the task will fail on each run
            public void accept(String data) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running ({})", data, count);
                throw new RuntimeException("test task failure");
            }

            @Override
            public String queueJob(String payload) {
                return null;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .retryInterval(Duration.ofSeconds(1))
                    .retryExponent(1.0)
                    .maxRetry(3) // specify a max-retry for test
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(2, TimeUnit.SECONDS)
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> signal.get() == 4); // initial attempt plus 3 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(5)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(6)) // timeout
            .until(() -> signal.get() == 4 ); // no increment of the signal shows job was not retried

        fixture.stop();
    }
}
