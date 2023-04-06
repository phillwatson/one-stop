package com.hillayes.rail.resource;

import javax.ws.rs.core.SecurityContext;
import java.util.UUID;

public abstract class AbstractResource {
    protected UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }
}
