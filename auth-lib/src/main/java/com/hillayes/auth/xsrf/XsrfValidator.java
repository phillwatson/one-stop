package com.hillayes.auth.xsrf;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.NewCookie;

/**
 * Generates and validates XSRF tokens. The validator will consider both the header and
 * cookie values in the request. If either is missing, or the two don't match, the
 * validation will fail.
 */
public interface XsrfValidator {
    public NewCookie generateCookie();

    public boolean validateToken(ContainerRequestContext request);
}
