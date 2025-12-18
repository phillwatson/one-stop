package com.hillayes.alphavantage.api.service;

import com.hillayes.alphavantage.api.domain.ApiFunction;
import com.hillayes.alphavantage.api.domain.DailyTimeSeries;
import com.hillayes.alphavantage.api.domain.TickerSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * https://www.alphavantage.co/documentation/
 */
@ApplicationScoped
@RegisterRestClient(configKey = "alpha-vantage-api")
@Path("/query")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AlphaVantageApi {
    @GET
    public DailyTimeSeries getDailySeries(@QueryParam("apikey") String apiKey,
                                          @QueryParam("function") ApiFunction function,
                                          @QueryParam("symbol") String stockSymbol);

    @GET
    public TickerSearchResponse symbolSearch(@QueryParam("apikey") String apiKey,
                                             @QueryParam("function") ApiFunction function,
                                             @QueryParam("keywords") String keywords);
}
