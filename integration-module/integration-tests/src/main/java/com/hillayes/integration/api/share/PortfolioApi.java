package com.hillayes.integration.api.share;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.*;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class PortfolioApi extends ApiBase {
    public PortfolioApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PortfolioResponse createPortfolio(PortfolioRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PortfolioResponse.class);
    }

    public PaginatedPortfolios getPortfolios(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedPortfolios.class);
    }

    public PortfolioResponse getPortfilio(UUID portfolioId) {
        return givenAuth()
            .get("/api/v1/shares/portfolios/{portfolioId}", portfolioId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PortfolioResponse.class);
    }

    public PortfolioResponse updatePortfolio(UUID portfolioId,
                                             PortfolioRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/shares/portfolios/{portfolioId}", portfolioId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PortfolioResponse.class);
    }

    public void deletePortfilio(UUID portfolioId) {
        givenAuth()
            .delete("/api/v1/shares/portfolios/{portfolioId}", portfolioId)
            .then()
            .statusCode(204)
            .contentType(JSON);
    }

    public HoldingResponse createShareTrade(UUID portfolioId,
                                            TradeRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/shares/portfolios/{portfolioId}/holdings", portfolioId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(HoldingResponse.class);
    }
}
