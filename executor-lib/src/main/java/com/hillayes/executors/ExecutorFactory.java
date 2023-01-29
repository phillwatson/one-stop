package com.hillayes.executors;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExecutorFactory {
    public static ExecutorService newExecutor(ExecutorConfiguration aConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Creating executorService: {}", aConfig);
        }

        ThreadFactory threadFactory = new DefaultThreadFactory(aConfig.getName());
        switch (aConfig.getExecutorType()) {
            case CACHED:
                return Executors.newCachedThreadPool(threadFactory);

            case SCHEDULED:
                return Executors.newScheduledThreadPool(aConfig.getNumberOfThreads(), threadFactory);

            case WORK_STEALING:
                return Executors.newWorkStealingPool(aConfig.getNumberOfThreads());

            case FIXED:
            default:
                return Executors.newFixedThreadPool(aConfig.getNumberOfThreads(), threadFactory);
        }
    }

    /**
     * A copy of the ThreadFactory implementation of Executors. This implementation
     * will set the name of the threads to match that of the configuration.
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String aName) {
            SecurityManager s = System.getSecurityManager();
            this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = aName + '-';
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            if (t.getPriority() != 5) {
                t.setPriority(5);
            }

            return t;
        }
    }
}
