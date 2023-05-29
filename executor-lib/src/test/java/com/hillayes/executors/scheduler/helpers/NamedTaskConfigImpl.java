package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import com.hillayes.executors.scheduler.config.RetryConfig;
import lombok.Builder;

import java.util.Optional;

@Builder
public class NamedTaskConfigImpl implements NamedTaskConfig {
    private FrequencyConfig frequency;
    private RetryConfig onFailure;
    private RetryConfig onIncomplete;

    @Override
    public Optional<FrequencyConfig> frequency() {
        return Optional.ofNullable(frequency);
    }

    @Override
    public Optional<RetryConfig> onFailure() {
        return Optional.ofNullable(onFailure);
    }

    @Override
    public Optional<RetryConfig> onIncomplete() {
        return Optional.ofNullable(onIncomplete);
    }
}
