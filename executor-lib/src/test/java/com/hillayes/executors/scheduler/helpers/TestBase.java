package com.hillayes.executors.scheduler.helpers;

import com.hillayes.executors.scheduler.config.SchedulerConfig;
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
}
