package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import lombok.Builder;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

@Builder
public class NamedTaskConfigImpl implements NamedTaskConfig {
    private FrequencyConfig frequency;

    @Override
    public Optional<FrequencyConfig> frequency() {
        return Optional.ofNullable(frequency);
    }

    @Override
    public Optional<Duration> retryInterval() {
        return Optional.empty();
    }

    @Override
    public OptionalDouble retryExponent() {
        return OptionalDouble.empty();
    }
}
