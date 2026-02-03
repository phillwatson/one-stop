package com.hillayes.integration.api.share;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.*;
import io.restassured.common.mapper.TypeRef;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class PortfolioApi extends ApiBase {
    private static final TypeRef<List<ShareTradeSummaryResponse>> SHARE_TRADE_SUMMARY_LIST = new TypeRef<>() {
    };

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

    public PortfolioResponse getPortfolio(UUID portfolioId) {
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

    public void deletePortfolio(UUID portfolioId) {
        givenAuth()
            .delete("/api/v1/shares/portfolios/{portfolioId}", portfolioId)
            .then()
            .statusCode(204)
            .contentType(JSON);
    }

    public List<ShareTradeSummaryResponse> getPortfolioHoldings(UUID portfolioId) {
        return givenAuth()
            .get("/api/v1/shares/portfolios/{portfolioId}/trades", portfolioId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(SHARE_TRADE_SUMMARY_LIST);
    }

    public ShareTradeResponse createShareTrade(UUID portfolioId,
                                               ShareTradeRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/shares/portfolios/{portfolioId}/trade", portfolioId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(ShareTradeResponse.class);
    }
}
