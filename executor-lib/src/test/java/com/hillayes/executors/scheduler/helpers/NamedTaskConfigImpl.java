package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import lombok.Builder;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Builder
public class NamedTaskConfigImpl implements NamedTaskConfig {
    private FrequencyConfig frequency;
    private Duration retryInterval;
    private Double retryExponent;

    @Override
    public Optional<FrequencyConfig> frequency() {
        return Optional.ofNullable(frequency);
    }

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
        return OptionalInt.empty();
    }
}
