package com.hillayes.commons.caching;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CacheTest {
    @Test
    public void testCache() {
        Cache<Integer, String> fixture = new Cache<>(Duration.ofMinutes(10));

        String value = fixture.getValueOrCall(1, key -> "value 1");
        assertEquals("value 1", value);
    }

    @Test
    public void testCacheTimeToLive() {
        Cache<Integer, String> fixture = new Cache<>(Duration.ofMillis(100));
        Function<Integer,String> supplier = spy(mockFunction("value 1"));

        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).apply(1);

        reset(supplier);
        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier, never()).apply(anyInt());

        // wait for cache to timeout
        sleep(110);
        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).apply(1);
    }

    @Test
    public void testCacheRemove() {
        Cache<Integer, String> fixture = new Cache<>(Duration.ofMillis(100));
        Function<Integer, String> supplier = spy(mockFunction("value 1"));

        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).apply(1);

        // remove entry
        assertEquals("value 1", fixture.remove(1));

        // entry was removed
        assertNull(fixture.remove(1));

        // getter will call supplier
        reset(supplier);
        assertEquals("value 1", fixture.getValueOrCall(1, supplier));
        verify(supplier).apply(1);
    }

    private Function<Integer, String> mockFunction(String value) {
        return new Function<Integer, String>() {
            public String apply(Integer key) {
                return value;
            }
        };
    }

    private void sleep(long milliseconds) {
        synchronized (this) {
            try {
                wait(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
