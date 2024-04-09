package com.hillayes.events.annotation;

import com.hillayes.commons.correlation.Correlation;
import com.hillayes.events.domain.EventPacket;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Optional;

/**
 * An AOP interceptor to set the correlation ID for the duration of the event
 * consumer method invocation.
 */
@TopicObserver
@Interceptor
public class TopicObserverInterceptor {
    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        String prevCorrelationId = findEventPacket(context)
            .map(EventPacket::getCorrelationId)
            .map(Correlation::setCorrelationId)
            .orElse(null);
        try {
            return context.proceed();
        } finally {
            Correlation.setCorrelationId(prevCorrelationId);
        }
    }

    private Optional<EventPacket> findEventPacket(InvocationContext context) {
        Object[] parameters = context.getParameters();
        if (parameters != null) {
            for (Object parameter : parameters) {
                if (parameter instanceof EventPacket) {
                    return Optional.of((EventPacket) parameter);
                }
            }
        }
        return Optional.empty();
    }
}
