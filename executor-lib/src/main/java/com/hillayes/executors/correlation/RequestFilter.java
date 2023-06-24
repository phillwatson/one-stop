package com.hillayes.executors.correlation;

import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Populates the correlation ID on the execution thread for incoming http requests,
 * and removes it when the request is complete.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class RequestFilter implements ContainerRequestFilter, ContainerResponseFilter {
    /**
     * The optional HTTP header, of the incoming request, from which a correlation
     * ID may be passed by the calling component. This is used for service-2-service
     * invocations, when a calling service wishes to correlate its actions with those
     * of this service.
     * <p>
     * see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List_of_HTTP_header_fields</a>
     * see <a href="https://hilton.org.uk/blog/microservices-correlation-id">microservices-correlation-id</a>
     */
    public static final String X_CORRELATION_ID = "X-Correlation-Id";


    /**
     * Called for incoming requests. It will set the request's correlation ID on
     * the thread. The correlation ID is either taken from the request headers or,
     * if not present, generated.
     *
     * @param requestContext request context.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationID = getCorrelationId(requestContext.getHeaders());
        Correlation.setCorrelationId(correlationID);
    }

    /**
     * Called for outgoing response. It will ensure that the correlation ID is
     * removed from the thread.
     *
     * @param requestContext  request context.
     * @param responseContext response context.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        Correlation.setCorrelationId(null);
    }

    /**
     * Retrieve a correlation ID. If the given request headers hold a correlation
     * ID then return that. Otherwise, a unique value is generated.
     *
     * @param headers the request headers from which the correlation ID may be obtained.
     * @return the derived or generated correlation ID.
     */
    private String getCorrelationId(MultivaluedMap<String, String> headers) {
        String correlationId = headers.getFirst(X_CORRELATION_ID);
        if (correlationId != null) {
            log.trace("Adopting correlation ID from headers: {}", correlationId);
            return correlationId;
        }

        correlationId = UUID.randomUUID().toString();
        log.trace("Generating new correlation ID: {}", correlationId);
        return correlationId;
    }
}
