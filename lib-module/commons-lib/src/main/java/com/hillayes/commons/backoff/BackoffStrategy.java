package com.hillayes.commons.backoff;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public abstract class BackoffStrategy<T> {
    private final T parameter;
    private final int maxAttempts;

    private ExceptionHandler exceptionHandler;

    protected BackoffStrategy(T param, int maxAttempts) {
        this.parameter = param;
        this.maxAttempts = maxAttempts;
    }

    /**
     * A convenience method to execute a task that does accept a parameter and
     * does not return a value.
     *
     * @param task the task to execute.
     */
    public void execute(Runnable task) {
        apply(p -> {
            task.run();
            return null;
        });
    }

    /**
     * A convenience method to execute a task that accepts the parameter given
     * in the BackoffStrategy constructor but does not return a value.
     *
     * @param task the task to be executed.
     */
    public void accept(Consumer<T> task) {
        apply(p -> {
            task.accept(p);
            return null;
        });
    }

    /**
     * Executes the given task passing the parameter given in the BackoffStrategy
     * constructor and returns the value returned by the task.
     *
     * @param task the task to be executed.
     * @return the result of the task.
     * @param <R> the return type.
     */
    public <R> R apply(Function<T, R> task) {
        int retryCount = 0;
        while (true) {
            try {
                return task.apply(parameter);
            } catch (Exception e) {
                log.debug("Caught exception [retryCount: {}]", retryCount, e);

                retryCount++;

                // if no exception handler is provided, or the exception handler returns false, then retry
                if ((exceptionHandler == null) || (!exceptionHandler.handle(e, retryCount))) {
                    if (retryCount >= maxAttempts) {
                        throw e;
                    }

                    try {
                        Thread.sleep(getRetryInterval(retryCount));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }
    }

    public BackoffStrategy setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    protected abstract long getRetryInterval(int attempts);

    /**
     * Defines a function that handles exceptions and returns whether to pause before
     * retrying the operation.
     */
    public interface ExceptionHandler {
        /**
         * Handle the exception and return whether to pause before retrying the operation.
         *
         * @param exception the exception that was thrown.
         * @param retryCount the number of times the operation has been tried so far.
         * @return true if the operation should be retried immediately, false if the operation
         *     should be retried after a delay, or throw an exception to stop retries.
         */
        boolean handle(Exception exception, int retryCount);
    }
}
