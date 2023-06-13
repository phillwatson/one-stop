package com.hillayes.rail.service;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public abstract class AbstractRailService {
    protected boolean isNotFound(WebApplicationException expection) {
        int status = expection.getResponse().getStatus();
        return status == Response.Status.NOT_FOUND.getStatusCode();
    }
}
