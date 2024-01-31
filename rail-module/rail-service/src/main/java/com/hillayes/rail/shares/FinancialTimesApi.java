package com.hillayes.rail.shares;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

public interface FinancialTimesApi {
    @GET
    @Path("historical")
    public String getHistoricalPrices(@QueryParam("s") String fund);
}
