package com.hillayes.auth.xsrf;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.IfBuildProfile;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.NewCookie;
import java.util.UUID;

@Dependent
@Slf4j
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
     * As the XSRF token is generated whenever a refresh-token is created, this
     * should be at least equal to the time-to-live duration of the refresh-token.
     */
    @ConfigProperty(name = "one-stop.auth.xsrf.duration-secs", defaultValue = "1800")
    private long timeoutSecs;

    @Produces
    @ApplicationScoped
    @DefaultBean
    public XsrfValidator xsrfGenerator(@ConfigProperty(name = "one-stop.auth.xsrf.secret") String secret) {
        log.info("Using XSRF default instance");
        XsrfGenerator result = new XsrfGenerator(secret);
        result.setCookieName(cookieName);
        result.setHeaderName(headerName);
        result.setTimeoutSecs(timeoutSecs);
        return result;
    }

    @Produces
    @ApplicationScoped
    @IfBuildProfile("test")
    public XsrfValidator xsrfTestInstance() {
        log.info("Using XSRF test instance");
        return new XsrfValidator() {
            @Override
            public boolean validateToken(ContainerRequestContext requestContext) {
                return true;
            }

            @Override
            public NewCookie generateCookie() {
                return new NewCookie(cookieName, UUID.randomUUID().toString());
            }
        };
    }
}
