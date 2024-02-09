package com.hillayes.sim.yapily;

import com.hillayes.sim.nordigen.AbstractEndpoint;
import com.hillayes.yapily.model.ApiListResponseOfInstitution;
import com.hillayes.yapily.model.ResponseListMeta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path(YapilySimulator.BASE_URI + "/institutions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InstitutionsEndpoint extends AbstractEndpoint {
    @GET
    public Response getInstitutions() throws IOException {
        log.info("listing institutions");
        ApiListResponseOfInstitution response = new ApiListResponseOfInstitution()
            .meta(new ResponseListMeta().count(0).tracingId(UUID.randomUUID().toString()))
            .data(List.of());
        return Response.ok(response).build();
    }

    @GET
    @Path("institutions/{id}")
    public Response getInstitution(@PathParam("id") String id) {
        log.info("get institution [id: {}]", id);
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public void reset() {
    }
}
