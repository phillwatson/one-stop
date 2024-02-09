package com.hillayes.sim.nordigen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.nordigen.model.Institution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Path(NordigenSimulator.BASE_URI + "/api/v2/institutions/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InstitutionsEndpoint extends AbstractEndpoint {
    private static final TypeReference<List<Institution>> INSTITUTION_LIST = new TypeReference<>() {};

    ObjectMapper objectMapper = MapperFactory.defaultMapper();

    @GET
    public Response list(@QueryParam("country") String countryCode,
                         @QueryParam("payments_enabled") Boolean paymentsEnabled) throws IOException {
        log.info("listing institutions [country: {}, payment_enabled: {}]", countryCode, paymentsEnabled);
        String filename = paymentsEnabled == Boolean.TRUE
            ? "/institutions-payments-enabled.json"
            : "/institutions-payments-disabled.json";
        URL resource = this.getClass().getResource(filename);

        List<Institution> institutions = objectMapper.readValue(resource, INSTITUTION_LIST);
        return Response.ok(institutions).build();
    }

    @GET
    @Path("{id}/")
    public Response get(@PathParam("id") String id) {
        log.info("get institution [id: {}]", id);
        String entity = DEFINITIONS.get(id);
        if (entity == null) {
            log.info("institution not found [id: {}]", id);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                    "summary", "Not found",
                    "detail", "Institution " + id + " not found"
                ))
                .build();
        }

        log.info("institution found [id: {}]", id);
        return Response.ok(entity).build();
    }

    public void reset() {
    }

    public final static Map<String,String> DEFINITIONS = Map.of(
        "SANDBOXFINANCE_SFIN0000", """
        {
            "id": "SANDBOXFINANCE_SFIN0000",
            "name": "Sandbox Finance",
            "bic": "SFIN0000",
            "transaction_total_days": "90",
            "countries": [
                "XX"
            ],
            "logo": "https://cdn.nordigen.com/ais/SANDBOXFINANCE_SFIN0000.png",
            "supported_payments": {},
            "supported_features": []
        }
        """,

        "FIRST_DIRECT_MIDLGB22", """
        {
            "id": "FIRST_DIRECT_MIDLGB22",
            "name": "First Direct",
            "bic": "MIDLGB22",
            "transaction_total_days": "730",
            "countries": [
                "GB"
            ],
            "logo": "https://cdn.nordigen.com/ais/FIRST_DIRECT_MIDLGB22.png",
            "supported_payments": {
                "single-payment": [
                    "FPS"
                ]
            },
            "supported_features": [
                "access_scopes",
                "business_accounts",
                "card_accounts",
                "corporate_accounts",
                "payments",
                "pending_transactions",
                "private_accounts",
                "submit_payment"
            ]
        }"""
    );
}
