package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedTask;
import com.hillayes.rail.config.ServiceConfiguration;
import io.quarkus.runtime.Startup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;

public class RailsScheduler {
    @Produces
    @ApplicationScoped
    @Startup
    public SchedulerFactory startScheduler(DataSource dataSource,
                                           ServiceConfiguration configuration,
                                           Instance<NamedTask> tasks) {
        return new SchedulerFactory(dataSource, configuration.scheduler(), tasks);
    }
}
