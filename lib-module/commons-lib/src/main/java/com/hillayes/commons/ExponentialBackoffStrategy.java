package com.hillayes.commons;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Builder
@Slf4j
public class ExponentialBackoffStrategy {
    @Builder.Default
    private int maxAttempts = 3;

    @Builder.Default
    private Duration retryInterval = Duration.ofSeconds(1);

    @Builder.Default
    private double retryExponent = 2.0;

    private BiFunction<Exception, Integer, Boolean> exceptionHandler;

    public <R> R execute(Supplier<R> task) {
        int retryCount = 0;
        while (true) {
            try {
                return task.get();
            } catch (Exception e) {
                log.debug("Caught exception [retryCount: {}]", retryCount, e);

                retryCount++;

                // if no exception handler is provided, or the exception handler returns false, then retry
                if ((exceptionHandler == null) || (! exceptionHandler.apply(e, retryCount))){
                    if (retryCount >= maxAttempts) {
                        throw e;
                    }

                    try {
                        Thread.sleep(getRetryInterval(retryCount));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }
    }

    private long getRetryInterval(int attempts) {
        return Math.round((double)this.retryInterval.toMillis() * Math.pow(this.retryExponent, attempts));
    }
}
