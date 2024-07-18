package com.hillayes.commons.backoff;

import java.time.Duration;

public class ExponentialBackoffStrategy<T> extends BackoffStrategy<T> {
    private final Duration retryInterval;
    private final double retryExponent;

    public ExponentialBackoffStrategy(T param, int maxAttempts) {
        this(param, Duration.ofSeconds(1), 2.0, maxAttempts);
    }

    public ExponentialBackoffStrategy(T param, Duration retryInterval,
                                      double retryExponent,
                                      int maxAttempts) {
        super(param, maxAttempts);
        this.retryInterval = retryInterval;
        this.retryExponent = retryExponent;
    }

    protected long getRetryInterval(int attempts) {
        return Math.round((double)this.retryInterval.toMillis() * Math.pow(this.retryExponent, attempts));
    }
}
