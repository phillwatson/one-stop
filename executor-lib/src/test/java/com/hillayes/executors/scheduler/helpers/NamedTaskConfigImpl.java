package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.FrequencyConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import lombok.Builder;

import java.util.Optional;

@Builder
public class NamedTaskConfigImpl implements NamedTaskConfig {
    private FrequencyConfig frequency;

    @Override
    public Optional<FrequencyConfig> frequency() {
        return Optional.ofNullable(frequency);
    }
}
