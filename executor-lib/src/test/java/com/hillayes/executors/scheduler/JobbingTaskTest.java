package com.hillayes.executors.scheduler;

import com.hillayes.executors.scheduler.helpers.NamedTaskConfigImpl;
import com.hillayes.executors.scheduler.helpers.RetryConfigImpl;
import com.hillayes.executors.scheduler.helpers.SchedulerConfigImpl;
import com.hillayes.executors.scheduler.helpers.TestBase;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class JobbingTaskTest extends TestBase {
    @Test
    public void testJobList() {
        final AtomicInteger signal = new AtomicInteger();

        NamedJobbingTask<String> task = new TestJobbingTask() {
            public TaskConclusion apply(TaskContext<String> context) {
                log.info("Task {} is running ({})", context.getPayload(), signal.incrementAndGet());
                return TaskConclusion.COMPLETE;
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
            .pollInterval(Duration.ofSeconds(5))
            .atMost(Duration.ofSeconds(20))
            .until(() -> signal.get() == 3);

        // queue some more jobs
        fixture.addJob(task, "four");
        fixture.addJob(task, "five");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(20))
            .until(() -> signal.get() == 5);

        fixture.stop();
    }

    @Test
    public void testOnFailureRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedJobbingTask<String> task = new TestJobbingTask() {
            public TaskConclusion apply(TaskContext<String> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                if (count < 3) {
                    throw new RuntimeException("test task failure");
                }
                return TaskConclusion.COMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .onFailure(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(25))
            .until(() -> signal.get() == 3);

        fixture.stop();
    }

    @Test
    public void testOnFailureMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedJobbingTask<String> task = new TestJobbingTask() {
            // the task will fail on each run
            public TaskConclusion apply(TaskContext<String> context) {
                assertEquals(signal.get(), context.getFailureCount());
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                throw new RuntimeException("test task failure");
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .onFailure(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .maxRetry(3) // specify a max-retry for test
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(() -> signal.get() == 4); // initial attempt plus 3 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(3)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(4)) // timeout
            .until(() -> signal.get() == 4 ); // no increment of the signal shows job was not retried

        fixture.stop();
    }

    @Test
    public void testRepeating() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean complete = new AtomicBoolean(false);

        NamedJobbingTask<String> task = new TestJobbingTask() {
            public TaskConclusion apply(TaskContext<String> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                complete.set(count >= 4);
                return complete.get() ? TaskConclusion.COMPLETE : TaskConclusion.INCOMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .onIncomplete(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .maxRetry(5)
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(25))
            .until(() -> signal.get() == 4 && complete.get());

        fixture.stop();
    }

    @Test
    public void testNoRepeating() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean complete = new AtomicBoolean(false);

        NamedJobbingTask<String> task = new TestJobbingTask() {
            public TaskConclusion apply(TaskContext<String> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                // never completes
                return TaskConclusion.INCOMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .onFailure(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .maxRetry(3) // less than completion condition
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .until(() -> signal.get() == 1); // initial attempt plus 3 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(5)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(6)) // timeout
            .until(() -> signal.get() == 1 ); // no increment of the signal shows job was not retried

        fixture.stop();
    }

    @Test
    public void testRepeatingMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean complete = new AtomicBoolean(false);

        NamedJobbingTask<String> task = new TestJobbingTask() {
            public TaskConclusion apply(TaskContext<String> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                // never completes
                return TaskConclusion.INCOMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .onIncomplete(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .maxRetry(3) // less than completion condition
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(() -> signal.get() == 4); // initial attempt plus 3 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(3)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(4)) // timeout
            .until(() -> signal.get() == 4 ); // no increment of the signal shows job was not retried

        fixture.stop();
    }

    @Test
    public void testRepeatingWithOnFailureMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean complete = new AtomicBoolean(false);

        NamedJobbingTask<String> task = new TestJobbingTask() {
            public TaskConclusion apply(TaskContext<String> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                // failure every other run
                // will fail 3 times - but won't reach max-retry due to reset on next repeat
                if (count % 2 == 0) {
                    throw new RuntimeException("Mock failure");
                }

                // will complete after 7 repeats
                complete.set(count >= 7);
                return complete.get() ? TaskConclusion.COMPLETE : TaskConclusion.INCOMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(task.getName(), NamedTaskConfigImpl.builder()
                    .onFailure(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .maxRetry(3)
                        .build())
                    .onIncomplete(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task));

        // queue some jobs
        fixture.addJob(task, "one");

        // wait for jobs to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(25))
            .until(() -> signal.get() == 7 && complete.get());

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(5)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(6)) // timeout
            .until(() -> signal.get() == 7 ); // no increment of the signal shows job was not retried

        fixture.stop();
    }

    private static abstract class TestJobbingTask implements NamedJobbingTask<String> {
        public String getName() {
            return "test-jobs";
        }

        @Override
        public String queueJob(String payload) {
            return null;
        }
    }
}