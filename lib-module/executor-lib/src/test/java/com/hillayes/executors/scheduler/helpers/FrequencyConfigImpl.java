package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.FrequencyConfig;
import lombok.Builder;

import java.time.Duration;
import java.util.Optional;

@Builder
public class FrequencyConfigImpl implements FrequencyConfig {
    private Duration recurs;
    private String timeOfDay;
    private String cron;

    @Override
    public Optional<Duration> recurs() {
        return Optional.ofNullable(recurs);
    }

    @Override
    public Optional<String> timeOfDay() {
        return Optional.ofNullable(timeOfDay);
    }

    @Override
    public Optional<String> cron() {
        return Optional.ofNullable(cron);
    }
}
