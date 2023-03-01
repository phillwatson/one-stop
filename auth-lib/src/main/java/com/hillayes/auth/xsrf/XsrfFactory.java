package com.hillayes.auth.xsrf;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class XsrfFactory {
    /**
     * The name of the cookie, in the incoming request, that holds the XSRF token
     * to be compared with that held in the named http header.
     */
    @ConfigProperty(name = "one-stop.auth.xsrf.cookie-name", defaultValue = "XSRF-TOKEN")
    String cookieName;

    /**
     * The name of the http header, in the incoming request, that holds the XSRF
     * token to be compared with that held in the named request cookie.
     */
    @ConfigProperty(name = "one-stop.auth.xsrf.header-name", defaultValue = "X-XSRF-TOKEN")
    String headerName;

    /**
     * The duration for which the generated XSRF token is valid - in seconds.
     */
    @ConfigProperty(name = "one-stop.auth.xsrf.duration-secs", defaultValue = "1800")
    private long timeoutSecs;

    @Produces
    public XsrfGenerator xsrfGenerator(@ConfigProperty(name = "one-stop.auth.xsrf.secret") String secret) {
        XsrfGenerator result = new XsrfGenerator(secret);
        result.setCookieName(cookieName);
        result.setHeaderName(headerName);
        result.setTimeoutSecs(timeoutSecs);
        return result;
    }
}
