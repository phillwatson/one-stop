package com.hillayes.executors;

/**
 * The type of ExecutorService to be used. The value maps to those types of
 * ExecutorService types supported by the java.util.concurrent.Executors class.
 */
public enum ExecutorType {
    CACHED,
    SCHEDULED,
    WORK_STEALING,
    FIXED;
}
