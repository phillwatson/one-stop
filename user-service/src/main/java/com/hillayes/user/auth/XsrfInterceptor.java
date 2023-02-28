package com.hillayes.user.auth;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.core.ResourceMethodInvoker;

import javax.annotation.PostConstruct;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

@Provider
@Slf4j
public class XsrfInterceptor implements ContainerRequestFilter {
    private static final Response ACCESS_UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();

    @ConfigProperty(name = "one-stop.xsrf.cookie-name")
    String xsrfTokenCookieName;

    @ConfigProperty(name = "one-stop.xsrf.header-name")
    String xsrfTokenHeaderName;

    @ConfigProperty(name = "one-stop.xsrf.duration-secs")
    long xsrfTokenDuration;

    private XsrfGenerator xsrfGenerator;

    @PostConstruct
    public void init() {
        log.trace("Creating XSRF Handler");
        xsrfGenerator = new XsrfGenerator(ConfigProvider.getConfig().getValue("one-stop.xsrf.secret", String.class));
        xsrfGenerator.setCookieName(xsrfTokenCookieName);
        xsrfGenerator.setHeaderName(xsrfTokenHeaderName);
        xsrfGenerator.setTimeout(xsrfTokenDuration * 1000);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        log.trace("Filtering XSRF tokens");
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();

        if (method.isAnnotationPresent(PermitAll.class)) {
            log.trace("All access permitted");
            return;
        }

        if (method.isAnnotationPresent(DenyAll.class)) {
            log.trace("All access denied");
            requestContext.abortWith(ACCESS_UNAUTHORIZED);
            return;
        }

        if ((method.isAnnotationPresent(RolesAllowed.class)) ||
            (method.getDeclaringClass().isAnnotationPresent(RolesAllowed.class))) {
            log.trace("Named roles allowed");
            // method requires authentication - so check xsrf token
            if (!xsrfGenerator.validateToken(requestContext)) {
                requestContext.abortWith(ACCESS_UNAUTHORIZED);
            }
        }
    }
}
