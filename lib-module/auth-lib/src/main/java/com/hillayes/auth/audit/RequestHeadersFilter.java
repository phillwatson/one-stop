package com.hillayes.auth.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.specimpl.UnmodifiableMultivaluedMap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A thread-safe utility to provide global access to the headers of the
 * current request. Allows access to the request headers from low-level
 * classes that do not have access to the request context.
 */
@ApplicationScoped
@Provider
public class RequestHeadersFilter implements ContainerRequestFilter, ContainerResponseFilter, RequestHeaders {
    // an empty, immutable instance of the request headers
    private static final MultivaluedMap<String, String> EMPTY
        = new UnmodifiableMultivaluedMap<>(new MultivaluedMapImpl<>());

    private static final ThreadLocal<MultivaluedMap<String, String>> requestHeaders = new ThreadLocal<>();
    private static final ThreadLocal<List<Locale>> acceptableLanguages = new ThreadLocal<>();

    /**
     * Filter method called before a request has been dispatched to a resource.
     * @param requestContext the incoming request context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestHeaders.set(new UnmodifiableMultivaluedMap<>(requestContext.getHeaders()));
        acceptableLanguages.set(requestContext.getAcceptableLanguages());
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
        acceptableLanguages.remove();
        requestHeaders.remove();
    }

    /**
     * Returns an unmodifiable map of the request headers. If no headers are present
     * then an empty map is returned.
     */
    public MultivaluedMap<String, String> getAll() {
        MultivaluedMap<String, String> map = requestHeaders.get();
        return (map == null) ? EMPTY : map;
    }

    /**
     * Gets the values of the named request header as a single string value.
     * If the message header is not present then null is returned. If the message header
     * is present but has no value then the empty string is returned. If the message
     * header is present more than once then the values of joined together and separated
     * by a ',' character.
     * @param headerName the name of the request header.
     * @return the named header value(s).
     */
    public List<String> get(String headerName) {
        MultivaluedMap<String, String> map = requestHeaders.get();
        return (map == null) ? List.of() : map.get(headerName);
    }

    /**
     * Returns the first value of the named request header, or null if no value is present.
     * @param headerName the name of the request header.
     * @return the named header value.
     */
    public String getFirst(String headerName) {
        MultivaluedMap<String, String> map = requestHeaders.get();
        return (map == null) ? null : map.getFirst(headerName);
    }

    /**
     * Get a list of media types that are acceptable for the response. The result is a
     * read-only list of requested response media types sorted according to their
     * q-value, with highest preference first.
     * @return the list of accepted languages, in order of preference.
     */
    public List<Locale> getAcceptableLanguages() {
        List<Locale> locales = acceptableLanguages.get();
        return locales == null ? List.of() : locales;
    }
}
