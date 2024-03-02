package com.hillayes.auth.xsrf;

import com.hillayes.auth.jwt.JwtTokens;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.core.ResourceMethodInvoker;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static com.hillayes.commons.Strings.isBlank;

/**
 * Intercepts all incoming HTTP requests and, if required, validates the XSRF token
 * contained in the request headers and access-token (or refresh-token). The request
 * is rejected if the token is deemed to be invalid.
 */
@ApplicationScoped
@Provider
@Slf4j
public class XsrfInterceptor implements ContainerRequestFilter {
    private static final Response ACCESS_UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();

    // method annotation for which no authentication is required
    private static final Collection<Class<? extends Annotation>> NO_AUTH = List.of(PermitAll.class);

    // method annotation for which no access is allows
    private static final Collection<Class<? extends Annotation>> DENY_ACCESS = List.of(DenyAll.class);

    // method and class annotations for which authentication is required
    // the XSRF token is checked in the access-token cookie
    private static final Collection<Class<? extends Annotation>> AUTH_REQUIRED = List.of(RolesAllowed.class, Authenticated.class);

    // method and class annotations for which XSRF authentication is required
    // the XSRF token is checked in the refresh-token cookie
    private static final Collection<Class<? extends Annotation>> XSRF_REQUIRED = List.of(XsrfRequired.class);

    @ConfigProperty(name = "one-stop.auth.access-token.cookie")
    Instance<String> accessCookieName;

    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    Instance<String> refreshCookieName;

    @ConfigProperty(name = "one-stop.auth.xsrf.header", defaultValue = "X-XSRF-TOKEN")
    Instance<String> xsrfHeaderName;

    @ConfigProperty(name = "one-stop.auth.refresh-token.expires-in")
    Instance<Duration> refreshDuration;

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

        if (isAnnotationPresent(methodAnnotations, NO_AUTH)) {
            log.trace("All access permitted");
            return;
        }

        if (isAnnotationPresent(methodAnnotations, DENY_ACCESS)) {
            log.trace("All access denied");
            requestContext.abortWith(ACCESS_UNAUTHORIZED);
            return;
        }

        if ((isAnnotationPresent(methodAnnotations, AUTH_REQUIRED)) ||
            (isAnnotationPresent(classAnnotations, AUTH_REQUIRED))) {
            log.trace("Method requires authentication");
            // check xsrf token in access-token cookie
            if (tokenIsInvalid(requestContext, accessCookieName.get())) {
                requestContext.abortWith(ACCESS_UNAUTHORIZED);
            }
        } else if ((isAnnotationPresent(methodAnnotations, XSRF_REQUIRED)) ||
            (isAnnotationPresent(classAnnotations, XSRF_REQUIRED))) {
            log.trace("Method requires XSRF validation");
            // check xsrf token in refresh-token cookie
            if (tokenIsInvalid(requestContext, refreshCookieName.get())) {
                requestContext.abortWith(ACCESS_UNAUTHORIZED);
            }
        }
    }

    private boolean isAnnotationPresent(Annotation[] annotations,
                                        Collection<Class<? extends Annotation>> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotationClass.contains(annotation.annotationType())) {
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
    private boolean tokenIsInvalid(ContainerRequestContext request,
                                   String cookieName) {
        log.trace("Validating XSRF tokens in context request");

        JsonWebToken accessToken = jwtTokens.getToken(cookieName, request.getCookies())
            .orElse(null);

        if (accessToken == null) {
            log.warn("XSRF token cookie missing [name: {}, path: {}]", cookieName, request.getUriInfo().getPath());
            return true;
        }

        String cookieValue = accessToken.getClaim("xsrf");
        if (isBlank(cookieValue)) {
            log.warn("XSRF token cookie blank [name: {}, path: {}]", cookieName, request.getUriInfo().getPath());
            return true;
        }

        List<String> headerList = request.getHeaders().get(xsrfHeaderName.get());
        if ((headerList == null) || (headerList.size() != 1)) {
            log.warn("XSRF token header invalid [name: {}, size: {}, path: {}]",
                xsrfHeaderName.get(), (headerList == null) ? 0 : headerList.size(),
                request.getUriInfo().getPath());
            return true;
        }

        String headerValue = headerList.getFirst();
        if (isBlank(headerValue)) {
            log.warn("XSRF token header blank [name: {}, path: {}]", xsrfHeaderName.get(), request.getUriInfo().getPath());
            return true;
        }

        return ! xsrfTokens.validateTokens(cookieValue, headerValue, refreshDuration.get());
    }
}
