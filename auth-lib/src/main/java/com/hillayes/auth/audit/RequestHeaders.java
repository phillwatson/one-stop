package com.hillayes.auth.audit;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.List;
import java.util.Locale;

/**
 * A thread-safe utility to provide global access to the headers of the
 * current request.
 */
public interface RequestHeaders {
    /**
     * Gets the values of the named request header as a single string value.
     * If the message header is not present then null is returned. If the message header
     * is present but has no value then the empty string is returned. If the message
     * header is present more than once then the values of joined together and separated
     * by a ',' character.
     * @param headerName the name of the request header.
     * @return the named header value(s).
     */
    public List<String> get(String headerName);

    /**
     * Returns the first value of the named request header, or null if no value is present.
     * @param headerName the name of the request header.
     * @return the named header value.
     */
    public String getFirst(String headerName);

    /**
     * Get a list of media types that are acceptable for the response. The result is a
     * read-only list of requested response media types sorted according to their
     * q-value, with highest preference first.
     * @return the list of accepted languages, in order of preference.
     */
    public List<Locale> getAcceptableLanguages();
}
