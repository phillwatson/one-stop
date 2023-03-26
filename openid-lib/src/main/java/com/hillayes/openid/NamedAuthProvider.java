package com.hillayes.openid;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A qualifier for naming an OpenID Auth Provider implementation, and
 * its configuration beans.
 *
 * See OpenIdFactory and OpenIdFactoryTest for examples of its use.
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface NamedAuthProvider {
    /**
     * The auth-provider on which to qualifier the injected bean.
     */
    AuthProvider value();
}
