package com.hillayes.executors.scheduler.helpers;

import javax.sql.DataSource;

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
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
