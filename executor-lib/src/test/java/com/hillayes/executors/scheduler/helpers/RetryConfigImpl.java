package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.RetryConfig;
import lombok.Builder;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Builder
public class RetryConfigImpl implements RetryConfig {
    private Duration retryInterval;
    private Double retryExponent;
    private Integer maxRetry;

    @Override
    public Optional<Duration> retryInterval() {
        return Optional.ofNullable(retryInterval);
    }

    @Override
    public OptionalDouble retryExponent() {
        return (retryExponent == null) ? OptionalDouble.empty() : OptionalDouble.of(retryExponent);
    }

    @Override
    public OptionalInt maxRetry() {
        return (maxRetry == null) ? OptionalInt.empty() : OptionalInt.of(maxRetry);
    }
}
