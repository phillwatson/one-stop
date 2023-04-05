package com.hillayes.rail.service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public abstract class AbstractRailService {
    protected boolean isNotFound(WebApplicationException expection) {
        int status = expection.getResponse().getStatus();
        return status == Response.Status.NOT_FOUND.getStatusCode();
    }
}
