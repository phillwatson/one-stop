package com.hillayes.executors.concurrent;

import lombok.Builder;
import lombok.Data;

/**
 * Records the configuration of one of many ExecutorServices created by the factory
 * ExecutorFactory. Each configuration is taken from the application properties:
 * <pre>
 *   executors:
 *     executor-name-1:
 *       executor-type: fixed
 *       number-of-threads: 12
 *     executor-name-2:
 *       executor-type: fixed
 *       number-of-threads: 6
 * </pre>
 */
@Data
@Builder
public class ExecutorConfiguration {
    private final String name;

    @Builder.Default
    private ExecutorType executorType = ExecutorType.FIXED;

    @Builder.Default
    private int numberOfThreads = 1;
}
