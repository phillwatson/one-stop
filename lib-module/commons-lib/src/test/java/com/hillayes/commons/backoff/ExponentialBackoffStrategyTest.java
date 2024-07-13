package com.hillayes.commons.backoff;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExponentialBackoffStrategyTest {
    @Test
    public void testMaxAttemptsExceeded() {
        BackoffStrategy<Void> fixture =
            new ExponentialBackoffStrategy(null, Duration.ofMillis(100), 1.0, 3);

        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(Exception.class, () ->
            fixture.execute(() -> {
                counter.getAndIncrement();
                throw new RuntimeException("Mock exception");
            })
        );

        assertEquals(3, counter.get());
    }

    @Test
    public void testExceptionHandler() {
        BackoffStrategy<Void> fixture =
            new ExponentialBackoffStrategy(null, Duration.ofMillis(100), 1.0, 3)
            .setExceptionHandler((e, retryCount) -> retryCount < 4);

        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(Exception.class, () ->
            fixture.execute(() -> {
                counter.getAndIncrement();
                throw new RuntimeException("Mock exception");
            })
        );

        assertEquals(4, counter.get());
    }
}
