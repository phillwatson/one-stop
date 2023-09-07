package com.hillayes.notification.resource;

import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

public class ResourceUtils {
    /**
     * Extracts any UUID of the calling user, from the given SecurityContext.
     */
    public static UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }
}
