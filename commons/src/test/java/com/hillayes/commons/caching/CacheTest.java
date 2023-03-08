package com.hillayes.commons.caching;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CacheTest {
    @Test
    public void testCache() {
        Cache<Integer, String> fixture = new Cache<>(Duration.ofMinutes(10).toMillis());

        String value = fixture.getValueOrCall(1, () -> "value 1");
        assertEquals("value 1", value);
    }

    @Test
    public void testCacheTimeToLive() {
        Cache<Integer, String> fixture = new Cache<>(Duration.ofMillis(100).toMillis());
        Supplier<String> supplier = spy(mockSupplier("value 1"));

        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).get();

        reset(supplier);
        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier, never()).get();

        // wait for cache to timeout
        sleep(100);
        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).get();
    }

    @Test
    public void testCacheRemove() {
        Cache<Integer, String> fixture = new Cache<>(Duration.ofMillis(100).toMillis());
        Supplier<String> supplier = spy(mockSupplier("value 1"));

        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).get();

        // remove entry
        assertEquals("value 1", fixture.remove(1));

        // entry was removed
        assertNull(fixture.remove(1));

        // getter will call supplier
        reset(supplier);
        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).get();
    }

    private Supplier<String> mockSupplier(String value) {
        return new Supplier<>() {
            public String get() {
                return value;
            }
        };
    }

    private void sleep(long milliseconds) {
        synchronized (this) {
            try {
                wait(milliseconds);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
