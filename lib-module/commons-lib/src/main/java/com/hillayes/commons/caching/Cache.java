package com.hillayes.commons.caching;

import java.time.Duration;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A simple cache to avoid making expensive calls for data that rarely changes.
 * We could use Quarkus @CacheResult, but we want it to have a time-to-live.
 *
 * The cache is not thread-safe. When multiple threads cache the same key
 * the result is uncertain. However, as the cache is intended for values
 * that are resolved from the same key, all threads should be attempting
 * to cache the same value.
 */
public class Cache<K,T> {
    /**
     * The lifespan for each cached entry - in milliseconds.
     */
    private final long timeToLive;

    /**
     * The cache of entries.
     */
    private final HashMap<K, Entry<T>> values = new HashMap<>();

    /**
     * Creates a new cache whose entries will expire the given number of
     * milliseconds after being cached.
     *
     * @param timeToLive the milliseconds before cached items expire.
     */
    public Cache(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * Creates a new cache whose entries will expire after the given duration
     * of being cached.
     *
     * @param timeToLive the duration after which cached items will expire.
     */
    public Cache(Duration timeToLive) {
        this(timeToLive.toMillis());
    }

    /**
     * Returns the value for the given key. If not cached, or expired, the given supplier
     * will be called, and it's value cached and returned.
     * If the supplier throws an exception, the cache will not be updated.
     * If the supplier returns null, the value will be not be cached.
     *
     * @param key the key of the cached value.
     * @param resolver the callback to call if the entry is not cached, or has expired.
     * @return the cached value, may be null if the supplier can return null.
     */
    public T getValueOrCall(K key, Function<K,T> resolver) {
        Entry<T> entry = values.get(key);
        if ((entry == null) || (entry.isExpired())) {
            T value = resolver.apply(key);
            if (value != null) {
                entry = new Entry<>(value, timeToLive);
                values.put(key, entry);
            }
        }
        return entry.value;
    }

    /**
     * Removes any cached entry that matches the given key value; returning the removed
     * entry.
     *
     * @param key the key of the entry to be removed.
     * @return the removed entry, or null.
     */
    public T remove(K key) {
        Entry<T> entry = values.remove(key);
        return entry == null ? null : entry.value;
    }

    private static class Entry<T> {
        private final long expires;
        private final T value;

        Entry(T value, long timeToLive) {
            expires = System.currentTimeMillis() + timeToLive;
            this.value = value;
        }

        public boolean isExpired() {
            return (expires <= System.currentTimeMillis());
        }

        public T getValue() {
            return value;
        }
    }
}
