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
This module is a facade on the kagkarlsson db-scheduler library. It
supports cron-style, repeating tasks and on-demand (Jobbing) tasks.
Tasks are shared across threads on those services that host the
scheduler.

#### Scheduled Tasks
Scheduled tasks are classes that implement the interface `NamedScheduledTask`
and run on a timer based frequency, declared in the configuration properties:
```yaml
    scheduler:
      tasks:
        poll-all-payments:
          frequency:
            time-of-day: "02:00:00"
```
These are useful for house-cleaning tasks, that need to be performed at
regular intervals.

#### Jobbing Tasks (on-demand)
Jobbing tasks are classes that implement the interface `NamedJobbingTask`
and are executed as and when they are requested. The method `queueJob()`
queues a new task instance passing any payload it requires. The scheduler
will execute it as soon as a thread is available.

These are useful when a service needs to performing a "long-running"
action as a result of a client request. The task can be offloaded to a
background thread to be run at some later time (preferably asap).

They are also useful to share the load of a bulk operation; allowing the
work to be shared across hosting services and their threads. For example;
a scheduled task may run to iterate over all payments and perform some action
on each of them. Rather than process each payment synchronously, it may
spawn/queue a Jobbing task for each payment.

These tasks can operate without any configuration, requiring only to implement
the interface `NamedJobbingTask`. However, configuration is required to support
additional functionality; such as retry-on-failure.

##### Correlation ID
Jobbing tasks run under the same correlation ID as the thread used to queue them.
Allowing a client request to be tracked to its final conclusion.

#### On-Failure Retry
All tasks (scheduled or Jobbing) can be configured to repeat if an exception
is raised during its execution.
```yaml
    poll-payment:
      on-failure:
        max-retry: 15
        retry-interval: PT1M
        retry-exponent: 1.5
```
##### On-Max-Retry - Chaining
It is possible to name another Jobbing task that will be queued should a
task (schedules or Jobbing) exceed the maximum number of failure retries. That
"chained" task will receive the same payload as the failed task, and can have
its own configuration.
```yaml
    poll-payment:
      on-failure:
        max-retry: 15
        retry-interval: PT1M
        retry-exponent: 1.5
        on-max-retry: cancel-payment
    
    cancel-payment:
      on-failure:
        max-retry: 2
        retry-interval: PT1M
        retry-exponent: 1
```

#### On-Incomplete Retry
Jobbing tasks support an additional configuration used when the task does not fail
(throw an exception) but is unable to complete.
```yaml
    poll-payment:
      on-failure:
        max-retry: 5
        retry-interval: PT1M
        retry-exponent: 2
      on-incomplete:
        max-retry: 15
        retry-interval: PT1M
        retry-exponent: 1.5
```
This is useful when the Jobbing task should poll for information but that information
is not available when the task is run.

The Jobbing tasks return a TaskConclusion enum value which indicates if it is COMPLETE
or INCOMPLETE. If INCOMPLETE, the task will be rescheduled according to the `on-incomplete`
configuration. If no `on-incomplete` configuration is provided, the task will simply end.

When used with `on-failure`, the `on-failure.max-retry` will only be applied to
consecutive failures (exceptions). Whenever the task returns INCOMPLETE, the failure
count will be reset.

##### On-Max-Retry - Chaining
The `on-incomplete` configuration also accepts the name of another task to be queued
when the initial Jobbing task exceeds the maximum number of incomplete retries. That
"chained" task will receive the same payload as the failed task, and can have its own
configuration.
```yaml
    poll-payment:
      on-incomplete:
        max-retry: 15
        retry-interval: PT1M
        retry-exponent: 1.5
        on-max-retry: cancel-payment
    
    cancel-payment:
      on-failure:
        max-retry: 2
        retry-interval: PT1M
        retry-exponent: 1
```

#### Database Schema
The scheduler stores the definitions for all tasks in a database table.
By default, the table is in the default schema of the database connection.
To select another schema, the schema can be declared in the configuration
property:
```yaml
    scheduler:
      schema: rails
```
