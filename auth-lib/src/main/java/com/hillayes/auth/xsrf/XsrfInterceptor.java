package com.hillayes.auth.xsrf;

import com.hillayes.auth.jwt.JwtTokens;
import io.smallrye.jwt.auth.principal.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.core.ResourceMethodInvoker;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

import static com.hillayes.commons.Strings.isBlank;

/**
 * Intercepts all incoming HTTP requests and, if required, validates the XSRF token
 * contained in the request headers and access-token. The request is rejected if the
 * token is deemed to be invalid.
 */
@ApplicationScoped
@Provider
@Slf4j
public class XsrfInterceptor implements ContainerRequestFilter {
    private static final Response ACCESS_UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();

    @ConfigProperty(name = "one-stop.auth.access-token.cookie")
    String accessCookieName;

    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    String refreshCookieName;

    @ConfigProperty(name = "one-stop.auth.xsrf.header-name", defaultValue = "X-XSRF-TOKEN")
    String xsrfHeaderName;

    @ConfigProperty(name = "one-stop.auth.refresh-token.duration-secs")
    int refreshDuration;

    @Inject
    XsrfTokens xsrfTokens;

    @Inject
    JwtTokens jwtTokens;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        log.trace("Filtering XSRF tokens");
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Annotation[] methodAnnotations = methodInvoker.getMethodAnnotations();
        Annotation[] classAnnotations = methodInvoker.getResourceClass().getAnnotations();

        if (isAnnotationPresent(methodAnnotations, PermitAll.class)) {
            log.trace("All access permitted");
            return;
        }

        if (isAnnotationPresent(methodAnnotations, DenyAll.class)) {
            log.trace("All access denied");
            requestContext.abortWith(ACCESS_UNAUTHORIZED);
            return;
        }

        if ((isAnnotationPresent(methodAnnotations, RolesAllowed.class)) ||
            (isAnnotationPresent(classAnnotations, RolesAllowed.class))) {
            log.trace("Method requires authentication");
            // check xsrf token in access-token cookie
            if (!validateToken(requestContext, accessCookieName)) {
                requestContext.abortWith(ACCESS_UNAUTHORIZED);
            }
        } else if ((isAnnotationPresent(methodAnnotations, XsrfRequired.class)) ||
            (isAnnotationPresent(classAnnotations, XsrfRequired.class))) {
            log.trace("Method requires XSRF validation");
            // check xsrf token in refresh-token cookie
            if (!validateToken(requestContext, refreshCookieName)) {
                requestContext.abortWith(ACCESS_UNAUTHORIZED);
            }
        }
    }

    private boolean isAnnotationPresent(Annotation[] methodAnnotations,
                                        Class<? extends Annotation> annotationClass) {
        for (Annotation annotation : methodAnnotations) {
            if (annotation.annotationType() == annotationClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides an entry point for validating an XSRF token passed on the header
     * properties of the given javax.ws.rs ContainerRequestContext.
     *
     * @param request the http request on which the XSRF token is expected.
     * @return true if the request contains a valid XSRF token.
     */
    public boolean validateToken(ContainerRequestContext request,
                                 String cookieName) {
        log.trace("Validating XSRF tokens in context request");

        JsonWebToken accessToken = jwtTokens.getToken(cookieName, request.getCookies())
            .orElse(null);

        if (accessToken == null) {
            log.warn("XSRF token cookie missing [name: {}]", cookieName);
            return false;
        }

        String cookieValue = accessToken.getClaim("xsrf");
        if (isBlank(cookieValue)) {
            log.warn("XSRF token cookie blank [name: {}]", cookieName);
            return false;
        }

        List<String> headerList = request.getHeaders().get(xsrfHeaderName);
        if ((headerList == null) || (headerList.size() != 1)) {
            log.warn("XSRF token header invalid [name: {}, size: {}]",
                xsrfHeaderName, (headerList == null) ? 0 : headerList.size());
            return false;
        }

        String headerValue = headerList.get(0);
        if (isBlank(headerValue)) {
            log.warn("XSRF token header blank [name: {}]", xsrfHeaderName);
            return false;
        }

        return xsrfTokens.validateTokens(cookieValue, headerValue, refreshDuration);
    }
}
