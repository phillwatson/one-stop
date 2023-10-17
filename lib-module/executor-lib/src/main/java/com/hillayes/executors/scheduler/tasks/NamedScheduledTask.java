package com.hillayes.executors.scheduler.tasks;

/**
 * A named instance of a named task that can be scheduled to run at configured
 * intervals.
 */
public interface NamedScheduledTask extends Runnable, NamedTask {
}
