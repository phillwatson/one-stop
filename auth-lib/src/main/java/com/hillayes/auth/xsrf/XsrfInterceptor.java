package com.hillayes.auth.xsrf;

import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.core.ResourceMethodInvoker;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

/**
 * Intercepts all incoming HTTP requests and, if required, validates the XSRF token
 * contained in the request headers and cookies. The request is rejected if the token
 * is deemed to be invalid.
 */
@Provider
@Slf4j
public class XsrfInterceptor implements ContainerRequestFilter {
    private static final Response ACCESS_UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();

    @Inject
    XsrfValidator xsrfValidator;

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
            if (!xsrfValidator.validateToken(requestContext)) {
                requestContext.abortWith(ACCESS_UNAUTHORIZED);
            }
        }
    }
}
