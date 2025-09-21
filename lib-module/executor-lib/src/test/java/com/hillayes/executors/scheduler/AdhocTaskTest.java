package com.hillayes.executors.scheduler;

import com.hillayes.executors.scheduler.helpers.NamedTaskConfigImpl;
import com.hillayes.executors.scheduler.helpers.RetryConfigImpl;
import com.hillayes.executors.scheduler.helpers.SchedulerConfigImpl;
import com.hillayes.executors.scheduler.helpers.TestBase;
import com.hillayes.executors.scheduler.tasks.AbstractNamedAdhocTask;
import com.hillayes.executors.scheduler.tasks.NamedAdhocTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
public class AdhocTaskTest extends TestBase {
    @Test
    public void testTaskList() {
        final AtomicInteger signal = new AtomicInteger();

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
            public TaskConclusion apply(TaskContext<String> context) {
                log.info("Task {} is running ({})", context.getPayload(), signal.incrementAndGet());
                return TaskConclusion.COMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Collections.emptyMap())
                .build(), List.of(task));

        // queue some tasks
        fixture.addTask(task, "one");
        fixture.addTask(task, "two");
        fixture.addTask(task, "three");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(3))
            .atMost(Duration.ofSeconds(20))
            .until(() -> signal.get() == 3);

        // queue some more tasks
        fixture.addTask(task, "four");
        fixture.addTask(task, "five");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(20))
            .until(() -> signal.get() == 5);

        fixture.stop();
    }

    /**
     * Ensures that simple object data types can be passed to tasks as JSON payloads.
     */
    @Test
    public void testUuidPayload() {
        final AtomicInteger signal = new AtomicInteger();

        UUID payload = UUID.randomUUID();

        NamedAdhocTask<UUID> task = new TestAdhocTask<>() {
            public TaskConclusion apply(TaskContext<UUID> context) {
                log.info("Task {} is running ({})", context.getPayload(), signal.incrementAndGet());
                assertEquals(payload, context.getPayload());
                return TaskConclusion.COMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Collections.emptyMap())
                .build(), List.of(task));

        // queue a task with a UUID payload
        fixture.addTask(task, payload);

        // wait for task to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(3))
            .atMost(Duration.ofSeconds(20))
            .until(() -> signal.get() == 1);

        fixture.stop();
    }

    @Test
    public void testVoidPayload() {
        final AtomicInteger signal = new AtomicInteger();

        NamedAdhocTask<Void> task = new TestAdhocTask<>() {
            public TaskConclusion apply(TaskContext<Void> context) {
                log.info("Task {} is running ({})", context.getPayload(), signal.incrementAndGet());
                assertNull(context.getPayload());
                return TaskConclusion.COMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Collections.emptyMap())
                .build(), List.of(task));

        // queue a task with a null payload
        fixture.addTask(task, null);

        // wait for task to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(3))
            .atMost(Duration.ofSeconds(20))
            .until(() -> signal.get() == 1);

        fixture.stop();
    }

    @Test
    public void testOnFailureRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
            public TaskConclusion apply(TaskContext<String> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                if (count < 3) {
                    throw new RuntimeException("Mock failure");
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

        // queue some tasks
        fixture.addTask(task, "one");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(25))
            .until(() -> signal.get() == 3);

        fixture.stop();
    }

    @Test
    public void testOnFailureMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
            // the task will fail on each run
            public TaskConclusion apply(TaskContext<String> context) {
                assertEquals(signal.get(), context.getFailureCount());
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count);

                throw new RuntimeException("Mock failure");
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

        // queue some tasks
        fixture.addTask(task, "one");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(() -> signal.get() == 4); // initial attempt plus 3 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(3)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(4)) // timeout
            .until(() -> signal.get() == 4 ); // no increment of the signal shows task was not retried

        fixture.stop();
    }

    @Test
    public void testOnFailureOnMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean maxRetrySignal = new AtomicBoolean();
        final ComplexPayload payload = new ComplexPayload(
            UUID.randomUUID(),
            RandomStringUtils.insecure().nextAlphanumeric(20)
        );

        NamedAdhocTask<ComplexPayload> onMaxRetryTask = new TestAdhocTask<>("on-max-retry-task") {
            // the task will run when max-retry is reached
            public TaskConclusion apply(TaskContext<ComplexPayload> context) {
                log.info("Running on-max-retry task: {}", context.getPayload());
                assertEquals(payload, context.getPayload());
                maxRetrySignal.set(true);
                return TaskConclusion.COMPLETE;
            }
        };

        NamedAdhocTask<ComplexPayload> task = new TestAdhocTask<>("task one") {
            // the task will fail on each run
            public TaskConclusion apply(TaskContext<ComplexPayload> context) {
                assertEquals(signal.get(), context.getFailureCount());
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {}, payload: {}])",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count, context.getPayload());

                assertEquals(payload, context.getPayload());
                throw new RuntimeException("Mock failure");
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(
                    onMaxRetryTask.getName(), NamedTaskConfigImpl.builder().build(),

                    task.getName(), NamedTaskConfigImpl.builder()
                    .onFailure(RetryConfigImpl.builder()
                        .retryInterval(Duration.ofSeconds(1))
                        .retryExponent(1.0)
                        .maxRetry(2) // specify a max-retry for test
                        .onMaxRetry(onMaxRetryTask.getName())
                        .build())
                    .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task, onMaxRetryTask));

        // queue some tasks
        fixture.addTask(task, payload);

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(() -> signal.get() == 3); // initial attempt plus 2 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(3)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(4)) // timeout
            .until(() -> signal.get() == 3 ); // no increment of the signal shows task was not retried

        // wait for on-max-retry task to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(maxRetrySignal::get);

        fixture.stop();
    }

    @Test
    public void testRepeating() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean complete = new AtomicBoolean(false);

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
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

        // queue some tasks
        fixture.addTask(task, "one");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(25))
            .until(() -> signal.get() == 4 && complete.get());

        fixture.stop();
    }

    @Test
    public void testNoRepeating() {
        final AtomicInteger signal = new AtomicInteger();

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
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

        // queue some tasks
        fixture.addTask(task, "one");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .until(() -> signal.get() == 1); // no retries on incomplete applied

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(5)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(6)) // timeout
            .until(() -> signal.get() == 1); // no increment of the signal shows task was not retried

        fixture.stop();
    }

    @Test
    public void testRepeatingMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
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

        // queue a task
        fixture.addTask(task, "one");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(15))
            .until(() -> signal.get() == 4); // initial attempt plus 3 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(3)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(4)) // timeout
            .until(() -> signal.get() == 4 ); // no increment of the signal shows task was not retried

        fixture.stop();
    }

    @Test
    public void testRepeatingWithOnFailureMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean complete = new AtomicBoolean(false);

        NamedAdhocTask<String> task = new TestAdhocTask<>() {
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

        // queue some tasks
        fixture.addTask(task, "one");

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(25))
            .until(() -> signal.get() == 7 && complete.get());

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(5)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(6)) // timeout
            .until(() -> signal.get() == 7 ); // no increment of the signal shows task was not retried

        fixture.stop();
    }

    @Test
    public void testOnRepeatingOnMaxRetry() {
        final AtomicInteger signal = new AtomicInteger();
        final AtomicBoolean maxRetrySignal = new AtomicBoolean();
        final ComplexPayload payload = new ComplexPayload(
            UUID.randomUUID(),
            RandomStringUtils.insecure().nextAlphanumeric(20)
        );

        NamedAdhocTask<ComplexPayload> onMaxRetryTask = new TestAdhocTask<>("on-max-retry-task") {
            // the task will run when max-retry is reached
            public TaskConclusion apply(TaskContext<ComplexPayload> context) {
                log.info("Running on-max-retry task: {}", context.getPayload());
                assertEquals(payload, context.getPayload());
                maxRetrySignal.set(true);
                return TaskConclusion.COMPLETE;
            }
        };

        NamedAdhocTask<ComplexPayload> task = new TestAdhocTask<>() {
            public TaskConclusion apply(TaskContext<ComplexPayload> context) {
                int count = signal.incrementAndGet();
                log.info("Task {} is running [failureCount: {}, repeatCount: {}, signal: {}, payload: {})",
                    context.getPayload(), context.getFailureCount(), context.getRepeatCount(), count, context.getPayload());

                assertEquals(payload, context.getPayload());

                // never completes
                return TaskConclusion.INCOMPLETE;
            }
        };

        SchedulerFactory fixture = new SchedulerFactory(getDatasource(),
            SchedulerConfigImpl.builder()
                .tasks(Map.of(
                    onMaxRetryTask.getName(), NamedTaskConfigImpl.builder().build(),

                    task.getName(), NamedTaskConfigImpl.builder()
                        .onIncomplete(RetryConfigImpl.builder()
                            .retryInterval(Duration.ofSeconds(1))
                            .retryExponent(1.0)
                            .maxRetry(2) // less than completion condition
                            .onMaxRetry(onMaxRetryTask.getName())
                            .build())
                        .build()))
                .pollingInterval(Duration.ofSeconds(1))
                .build(),
            List.of(task, onMaxRetryTask));

        // queue some tasks
        fixture.addTask(task, payload);

        // wait for tasks to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(() -> signal.get() == 3); // initial attempt plus 2 retries

        // no more retry attempts will be made
        Awaitility.await()
            .pollDelay(Duration.ofSeconds(3)) // allow time for another retry (which shouldn't happen)
            .atMost(Duration.ofSeconds(4)) // timeout
            .until(() -> signal.get() == 3 ); // no increment of the signal shows task was not retried

        // wait for on-max-retry task to complete
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(12))
            .until(maxRetrySignal::get);

        fixture.stop();
    }

    private static abstract class TestAdhocTask<T> extends AbstractNamedAdhocTask<T> {
        TestAdhocTask() {
            this("test-tasks");
        }

        TestAdhocTask(String name) {
            super(name);
        }
    }

    private static record ComplexPayload(
        UUID id,
        String name
    ) {}
}
