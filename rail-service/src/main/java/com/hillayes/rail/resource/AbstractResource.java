package com.hillayes.rail.resource;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.util.UUID;

@Slf4j
public abstract class AbstractResource {
    protected UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }

    protected void logHeaders(HttpHeaders    headers) {
        headers.getRequestHeaders().forEach((k, v) -> log.debug("Header: {} = \"{}\"", k, v));
    }
}
