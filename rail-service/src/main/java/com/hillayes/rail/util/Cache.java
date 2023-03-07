package com.hillayes.rail.util;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * A simple cache to avoid calling making expensive calls for data that rarely
 * changes. We could use Quarkus @CacheResult, but we want it to have a time-to-live.
 */
public class Cache<K,T> {
    /**
     * The lifespan for each cached entry - in milliseconds.
     */
    private final long timeToLive;

    /**
     * The cache of entries.
     */
    private final HashMap<K, Entry<T>> value = new HashMap<>();

    public Cache(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * Returns the value for the given key. If not cached, or expired, the given supplier
     * will be called, and it's value cached and returned.
     *
     * @param key the key of the cached value.
     * @param supplier the callback to call if the entry is not cached, or has expired.
     * @return the cached value,
     */
    public T getValueOrCall(K key, Supplier<T> supplier) {
        Entry<T> entry = value.get(key);
        if ((entry == null) || (entry.isExpired(timeToLive))) {
            entry = new Entry<>(supplier.get());
            value.put(key, entry);
        }
        return entry.value;
    }

    private static class Entry<T> {
        private final long created;
        private final T value;

        Entry(T value) {
            created = System.currentTimeMillis();
            this.value = value;
        }

        public boolean isExpired(long timeToLive) {
            return (created + timeToLive < System.currentTimeMillis());
        }

        public T getValue() {
            return value;
        }
    }
}
