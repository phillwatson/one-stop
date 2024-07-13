package com.hillayes.commons;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExponentialBackoffStrategyTest {
    @Test
    public void testMaxAttemptsExceeded() {
        ExponentialBackoffStrategy fixture = ExponentialBackoffStrategy.builder()
            .maxAttempts(3)
            .retryInterval(Duration.ofMillis(100))
            .retryExponent(1.0)
            .build();

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
        ExponentialBackoffStrategy fixture = ExponentialBackoffStrategy.builder()
            .maxAttempts(3)
            .retryInterval(Duration.ofMillis(100))
            .retryExponent(1.0)
            .exceptionHandler((e, retryCount) -> retryCount < 4)
            .build();

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
