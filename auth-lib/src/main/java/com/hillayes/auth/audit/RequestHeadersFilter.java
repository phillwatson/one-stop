package com.hillayes.auth.audit;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import java.io.IOException;

/**
 * A thread-safe utility to provide global access to the headers of the
 * current request.
 */
public class RequestHeadersFilter implements ContainerRequestFilter, ContainerResponseFilter {
    /**
     * Filter method called before a request has been dispatched to a resource.
     * @param requestContext the incoming request context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RequestHeaders.startRequest(requestContext);
    }

    /**
     * Filter method called after a response has been provided for a request (either by a
     * request filter or by a matched resource method.
     * @param requestContext the incoming request context.
     * @param responseContext the outgoing response context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        RequestHeaders.endRequest();
    }
}
