package com.hillayes.auth.xsrf;

import java.lang.annotation.*;

/**
 * Marks the method or class as requiring an XSRF token validation.
 * All methods annotated with a RolesAllowed annotation will authomatically
 * require XSRF token validation.
 * This is useful when no RolesAllowed annotation is present, but the method
 * still requires an XSRF token.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XsrfRequired {
}
