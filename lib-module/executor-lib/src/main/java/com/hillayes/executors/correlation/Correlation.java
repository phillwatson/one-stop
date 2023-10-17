package com.hillayes.executors.correlation;

import org.jboss.logmanager.MDC;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages the assignment of correlation IDs on threads, and it's inclusion in
 * logging messages.
 */
public class Correlation {
    /**
     * The key by which the correlation ID can be referenced in the log appender
     * pattern - %X{correlationId}
     */
    public static final String CORRELATION_KEY = "correlationId";

    /**
     * A utility method to return the calling thread' correlation ID. The result
     * may be null.
     */
    public static Optional<String> getCorrelationId() {
        return Optional.ofNullable(MDC.get(CORRELATION_KEY));
    }

    /**
     * Sets the correlation ID of the current thread, returning the previous ID (which may
     * be null).
     *
     * @param correlationId the correlation ID to be set (can be null).
     * @return the previous ID (can be null).
     */
    public static String setCorrelationId(String correlationId) {
        if (correlationId == null) {
            return MDC.remove(CORRELATION_KEY);
        } else {
            return MDC.put(CORRELATION_KEY, correlationId);
        }
    }

    /**
     * A utility method to call the given Callable function, setting the given correlation
     * ID for the duration of the call. Restores any previous correlation ID when the function
     * is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aCallable the function to be called.
     * @param <T> the function's return type.
     * @return the function's return value.
     * @throws Exception if the function throws an exception.
     */
    public static <T> T call(String aCorrelationId, Callable<T> aCallable) throws Exception {
        String prevId = setCorrelationId(aCorrelationId);
        try {
            return aCallable.call();
        } finally {
            setCorrelationId(prevId);
        }
    }

    /**
     * A utility method to call the given Runnable function, setting the given correlation
     * ID for the duration of the call. Restores any previous correlation ID when the function
     * is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aRunnable the function to be called.
     */
    public static void run(String aCorrelationId, Runnable aRunnable) {
        String prevId = setCorrelationId(aCorrelationId);
        try {
            aRunnable.run();
        } finally {
            setCorrelationId(prevId);
        }
    }

    /**
     * A utility method to call the given Consumer function with the given argument,
     * setting the given correlation ID for the duration of the call. Restores any previous
     * correlation ID when the function is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aConsumer the function to be called.
     * @param aArg the function's argument value.
     * @param <T> the function's argument type.
     */
    public static <T> void call(String aCorrelationId, Consumer<T> aConsumer, T aArg) {
        String prevId = setCorrelationId(aCorrelationId);
        try {
            aConsumer.accept(aArg);
        } finally {
            setCorrelationId(prevId);
        }
    }

    /**
     * A utility method to call the given function with the given argument and return its
     * result, setting the given correlation ID for the duration of the call. Restores any
     * previous correlation ID when the function is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aFunction the function to be called.
     * @param aArg the function's argument value.
     * @param <T> the function's argument type.
     * @param <R> the function's return type.
     *
     */
    public static <T,R> R call(String aCorrelationId, Function<T,R> aFunction, T aArg) {
        String prevId = setCorrelationId(aCorrelationId);
        try {
            return aFunction.apply(aArg);
        } finally {
            setCorrelationId(prevId);
        }
    }
}
