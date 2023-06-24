package com.hillayes.auth.audit;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.List;
import java.util.Locale;

/**
 * A thread-safe utility to provide global access to the headers of the
 * current request.
 */
public class RequestHeaders {
    private static final ThreadLocal<MultivaluedMap<String, String>> requestHeaders = new ThreadLocal<>();
    private static final ThreadLocal<List<Locale>> acceptableLanguages = new ThreadLocal<>();

    static void startRequest(ContainerRequestContext requestContext) {
        requestHeaders.set(requestContext.getHeaders());
        acceptableLanguages.set(requestContext.getAcceptableLanguages());
    }

    static void endRequest() {
        acceptableLanguages.remove();
        requestHeaders.remove();
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
    public static List<String> get(String headerName) {
        MultivaluedMap<String, String> map = requestHeaders.get();
        if (map == null) {
            return List.of();
        }

        return map.get(headerName);
    }

    /**
     * Returns the first value of the named request header, or null if no value is present.
     * @param headerName the name of the request header.
     * @return the named header value.
     */
    public static String getFirst(String headerName) {
        MultivaluedMap<String, String> map = requestHeaders.get();
        if (map == null) {
            return null;
        }

        return map.getFirst(headerName);
    }

    /**
     * Get a list of media types that are acceptable for the response. The result is a
     * read-only list of requested response media types sorted according to their
     * q-value, with highest preference first.
     * @return the list of accepted languages, in order of preference.
     */
    public static List<Locale> getAcceptableLanguages() {
        List<Locale> locales = acceptableLanguages.get();
        return locales == null ? List.of() : locales;
    }
}
