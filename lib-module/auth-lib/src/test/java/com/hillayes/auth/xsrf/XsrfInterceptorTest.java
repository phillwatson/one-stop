package com.hillayes.auth.xsrf;

import com.hillayes.auth.jwt.JwtTokens;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class XsrfInterceptorTest {
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String XSRF_HEADER = "XSRF-TOKEN";

    private final XsrfTokens xsrfGenerator = new XsrfTokens(UUID.randomUUID().toString());

    private final MeterRegistry metrics = mock();
    private final Counter authFailureCounter = mock();

    private XsrfInterceptor fixture;

    @BeforeEach
    public void beforeEach() throws Exception {
        when(metrics.counter(any())).thenReturn(authFailureCounter);

        JwtTokens jwtTokens = mock();
        when(jwtTokens.getToken(any(), any())).thenAnswer(invocation -> {
            Map<String, Cookie> cookies = invocation.getArgument(1);
            Cookie cookie = cookies.get(ACCESS_TOKEN);
            if (cookie == null) {
                return Optional.empty();
            }

            JsonWebToken jwt = mock(JsonWebToken.class);
            when(jwt.getClaim("xsrf")).thenReturn(cookie.getValue());
            return Optional.of(jwt);
        });

        fixture = new XsrfInterceptor(
            Optional.of(ACCESS_TOKEN),
            Optional.of(REFRESH_TOKEN),
            Optional.of(XSRF_HEADER),
            Optional.of(Duration.ofSeconds(60)),
            xsrfGenerator,
            jwtTokens,
            metrics
        );
    }

    @Test
    public void testValidationHeaders() {
        String token = xsrfGenerator.generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token);
        mockXsrfHeader(requestContext, token);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void testMissingCookie() {
        String token = xsrfGenerator.generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, null);
        mockXsrfHeader(requestContext, token);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    @Test
    public void testTooManyHeaders() {
        String token = xsrfGenerator.generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token);
        mockXsrfHeader(requestContext, token, "second-value");
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    @Test
    public void testMissingHeader() {
        String token = xsrfGenerator.generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token);
        mockXsrfHeader(requestContext);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    @Test
    public void testTimeout() throws InterruptedException {
        XsrfTokens generator = new XsrfTokens("this is a different secret");

        String token = generator.generateToken();
        synchronized (this) {
            wait(1000);
        }

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token);
        mockXsrfHeader(requestContext, token);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.refreshDuration = Optional.of(Duration.ofSeconds(1));
        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    @Test
    public void testTokenMismatch_sameHandler() {
        String token1 = xsrfGenerator.generateToken();
        String token2 = xsrfGenerator.generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token1);
        mockXsrfHeader(requestContext, token2);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    @Test
    public void testTokenMismatch_differentHandler_differentSecret() {
        String token1 = xsrfGenerator.generateToken();
        String token2 = new XsrfTokens("this is a different secret").generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token1);
        mockXsrfHeader(requestContext, token2);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    @Test
    public void testTokenMismatch_differentHandler_sameSecret() {
        String token1 = new XsrfTokens("this is a secret").generateToken();
        String token2 = new XsrfTokens("this is a secret").generateToken();

        ContainerRequestContext requestContext = mock();
        mockXsrfCookie(requestContext, token1);
        mockXsrfHeader(requestContext, token2);
        mockMethod(requestContext, mockRolesAllowed());
        mockUriInfo(requestContext);

        fixture.filter(requestContext);

        verify(requestContext).abortWith(any());
        verify(authFailureCounter).increment();
    }

    private void mockUriInfo(ContainerRequestContext requestContext) {
        UriInfo uriInfo = mock();
        when(uriInfo.getPath()).thenReturn("/mock/uri/path");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    private void mockXsrfCookie(ContainerRequestContext requestContext, String xsrfToken) {
        Map<String, Cookie> cookies = (xsrfToken == null)
            ? Map.of()
            : Map.of(ACCESS_TOKEN, new Cookie.Builder(ACCESS_TOKEN).value(xsrfToken).build());
        when(requestContext.getCookies()).thenReturn(cookies);
    }

    private void mockXsrfHeader(ContainerRequestContext requestContext, String ... xsrfTokens) {
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        if ((xsrfTokens != null) && (xsrfTokens.length > 0)){
            headers.put(XSRF_HEADER, List.of(xsrfTokens));
        }
        when(requestContext.getHeaders()).thenReturn(headers);
    }

    private void mockMethod(ContainerRequestContext requestContext,
                            Annotation annotation) {
        ResourceMethodInvoker methodInvoker = mock();

        when(methodInvoker.getResourceClass()).then(invocation -> Object.class); // any class will do
        when(methodInvoker.getMethodAnnotations()).thenReturn(new Annotation[] { annotation } );
        when(requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker")).thenReturn(methodInvoker);
    }

    private Annotation mockRolesAllowed() {
        return new RolesAllowed() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RolesAllowed.class;
            }

            @Override
            public String[] value() {
                return new String[0];
            }
        };
    }
}
