package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.ScheduleConfig;
import com.hillayes.executors.scheduler.config.NamedTaskConfig;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public abstract class TestBase {
    private static volatile DataSource ds;

    public static DataSource getDatasource() {
        if (ds == null) {
            synchronized (TestBase.class) {
                if (ds == null) {
                    ds = TestDatasource.initDatabase();
                }
            }
        }
        return ds;
    }

    protected void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ScheduleConfig mockConfiguration(Map<String, NamedTaskConfig> taskConfigs) {
        return new ScheduleConfig() {
            @Override
            public Optional<Integer> threadCount() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> pollingInterval() {
                return Optional.empty();
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
                return taskConfigs;
            }
        };
    }
}
