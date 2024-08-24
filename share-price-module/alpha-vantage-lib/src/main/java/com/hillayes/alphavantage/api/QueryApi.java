package com.hillayes.alphavantage.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * https://www.alphavantage.co/documentation/
 */
@RegisterRestClient(configKey = "alpha-vantage-api")
@RegisterProvider(RequestApiKeyProvider.class)
@Path("/query")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface QueryApi {
    /**
     *
     * Example:
     * https://www.alphavantage.co/query?function=TIME_SERIES_MONTHLY_ADJUSTED&symbol=TW.LON&apikey=4XXXXXVIXXXRXXXU
     *
     * @param timeSeries The time series function to be applied.
     * @param stockSymbol The stock symbol to be queried.
     * @return
     */
    @GET
    public Object getPriceHistory(@QueryParam("function") TimeSeries timeSeries,
                                  @QueryParam("symbol") String stockSymbol);
}
