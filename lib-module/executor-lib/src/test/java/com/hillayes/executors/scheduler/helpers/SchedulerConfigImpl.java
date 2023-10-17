package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.NamedTaskConfig;
import com.hillayes.executors.scheduler.config.SchedulerConfig;
import lombok.Builder;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Builder
public class SchedulerConfigImpl implements SchedulerConfig {
    private Map<String, NamedTaskConfig> tasks;
    private Duration pollingInterval;

    @Override
    public Optional<String> schema() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> threadCount() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> pollingInterval() {
        return Optional.ofNullable(pollingInterval);
    }

    @Override
    public Optional<Duration> heartbeatInterval() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> shutdownMaxWait() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> unresolvedTimeout() {
        return Optional.empty();
    }

    @Override
    public Map<String, NamedTaskConfig> tasks() {
        return tasks;
    }
}
