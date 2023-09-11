package com.hillayes.executors.scheduler;

import com.hillayes.executors.scheduler.config.SchedulerConfig;
import com.hillayes.executors.scheduler.tasks.NamedTask;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import javax.sql.DataSource;

public class Starter {
    @Produces
    @ApplicationScoped
    @Startup
    public SchedulerFactory startScheduler(DataSource dataSource,
                                           SchedulerConfig configuration,
                                           Instance<NamedTask> tasks) {
        return new SchedulerFactory(dataSource, configuration, tasks);
    }
}
