# One-Stop

---

## Executor-Lib
Provides a collection of functionality related to background tasks and
correlation IDs.

### Correlation
This provides a unique ID for each request, to be included in log
entries, allowing service requests to be traced from beginning to end.

Incoming requests are filtered to see if the client has included a
correlation ID in the request headers (name "X-Correlation-Id"). If so,
that ID will be placed on the request thread's MDC (Mapped Diagnostic
Context). If not present a new correlation ID will be used instead.

The utility class `com.hillayes.executors.correlation.Correlation`
provides methods access the correlation ID and to propagate it to other
threads.

### Scheduler
This package is a facade on the kagkarlsson db-scheduler library. It
supports cron-style, repeating tasks and on-demand (jobbing) tasks.
Tasks are shared across threads on those services that host the
scheduler.

#### Scheduled Tasks
Scheduled tasks are classes that implement the interface `NamedScheduledTask`
and run on a timer based frequency, declared in the configuration properties: 
```
    scheduler:
      tasks:
        <task name>:
          frequency:
            time-of-day: "02:00:00"
```
These are useful for house-cleaning tasks, that need to be performed at
regular intervals.

#### Jobbing Tasks (on-demand)
Jobbing tasks are classes that implement the interface `NamedJobbingTask`
and are executed as and when they are requested. The method `queue()`
queues a new task instance passing any payload it requires. The scheduler
will execute it as soon as a thread is available.

These are useful when a service needs to avoid performing a "long-running"
action as a result of a client request. The task can be offloaded to a
background thread to be run at some later time (preferably asap).

They are also useful to share the load of a bulk operation; allowing the
work to be shared across hosting services and their threads. For example;
a scheduled task may run to iterate over all accounts and perform some action
on them. Rather than perform those action synchronously, it may spawn/queue a
Jobbing task for each account.

These tasks can operate without any configuration, requiring only to
implement the interface `NamedJobbingTask`. However, configuration is required
to support additional functionality; such as on-failure actions.

#### Retry On-Failure
All tasks (scheduled or jobbing) can be configured to repeat if an exception
is raised during its execution.
```
        poll-account:
          on-failure: # gives a max of 4 hours 53 minutes
            max-retry: 15
            retry-interval: PT1M
            retry-exponent: 1.5
```

#### On-Max-Retry
A task (scheduled or jobbing) can be configured to run another Jobbing task
should it fail to complete after the configured on-failure settings. For
example; the configuration below would run the task `account-poll-failed`
if the `poll-account` tasks failed 15 times.
```
        poll-account:
          on-failure: # gives a max of 4 hours 53 minutes
            max-retry: 15
            retry-interval: PT1M
            retry-exponent: 1.5
            on-max-retry: account-poll-failed
```

#### Database Schema
The scheduler stores the definitions for all tasks in a database table.
By default, the table is in the default schema of the database connection.
To select another schema, the schema can be declared in the configuration
property:
```
    scheduler:
      schema: rails
```
