package com.hillayes.shares.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.shares.domain.*;
import com.hillayes.shares.service.PortfolioService;
import com.hillayes.shares.service.SharePriceService;
import com.hillayes.shares.service.ShareTradeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

@Path("/api/v1/shares/portfolios")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class PortfolioResource {
    private final PortfolioService portfolioService;
    private final SharePriceService sharePriceService;
    private final ShareTradeService shareTradeService;

    @GET
    public Response getPortfolios(@Context SecurityContext ctx,
                                  @Context UriInfo uriInfo,
                                  @QueryParam("page")@DefaultValue("0") int pageIndex,
                                  @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Listing portfolios [userId: {}, page: {}, pageSize: {}",
            userId, pageIndex, pageSize);

        Page<Portfolio> portfolios = portfolioService.listPortfolios(userId, pageIndex, pageSize);

        PaginatedPortfolios response = new PaginatedPortfolios()
            .page(portfolios.getPageIndex())
            .pageSize(portfolios.getPageSize())
            .count(portfolios.getContentSize())
            .total(portfolios.getTotalCount())
            .totalPages(portfolios.getTotalPages())
            .items(portfolios.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, portfolios));

        if (log.isDebugEnabled()) {
            log.debug("Listing portfolios [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, pageIndex, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/{portfolioId}")
    @Transactional
    public Response getPortfolio(@Context SecurityContext ctx,
                                 @PathParam("portfolioId") UUID portfolioId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);

        Portfolio portfolio = portfolioService.getPortfolio(userId, portfolioId)
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        log.debug("Retrieved portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);
        return Response.ok(marshalDetail(portfolio)).build();
    }

    @POST
    @Transactional
    public Response createPortfolio(@Context SecurityContext ctx,
                                    PortfolioRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Create portfolio [userId: {}, portfolioName: {}]", userId, request.getName());

        Portfolio portfolio = portfolioService.createPortfolio(userId, request.getName());

        if (log.isDebugEnabled()) {
            log.debug("Created portfolio [userId: {}, portfolioId: {}, portfolioName: {}]",
                userId, portfolio.getUserId(), portfolio.getName());
        }
        return Response.ok(marshal(portfolio)).build();
    }

    @PUT
    @Path("/{portfolioId}")
    @Transactional
    public Response updatePortfolio(@Context SecurityContext ctx,
                                    @PathParam("portfolioId") UUID portfolioId,
                                    PortfolioRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Updating portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);

        Portfolio portfolio = portfolioService.updatePortfolio(userId, portfolioId, request.getName())
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        log.debug("Updated portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);
        return Response.ok(marshal(portfolio)).build();
    }

    @DELETE
    @Path("/{portfolioId}")
    public Response deletePortfolio(@Context SecurityContext ctx,
                                    @PathParam("portfolioId") UUID portfolioId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);

        portfolioService.deletePortfolio(userId, portfolioId)
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        log.debug("Deleted portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{portfolioId}/holdings")
    public Response recordShareTrade(@Context SecurityContext ctx,
                                     @PathParam("portfolioId") UUID portfolioId,
                                     TradeRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Creating a share trade [userId: {}, portfolioId: {}, isin: {}, ticker: {}, quantity: {}]",
            userId, portfolioId, request.getShareId().getIsin(), request.getShareId().getTickerSymbol(), request.getQuantity());

        Holding holding = shareTradeService.recordShareTrade(
            userId, portfolioId, request.getDateExecuted(),
            ShareIndex.ShareIdentity.builder()
                .isin(request.getShareId().getIsin())
                .tickerSymbol(request.getShareId().getTickerSymbol())
                .build(),
            request.getQuantity(),
            BigDecimal.valueOf(request.getPricePerShare()));

        if (log.isDebugEnabled()) {
            log.debug("Created a share trade [userId: {}, portfolioId: {}, isin: {}, ticker: {}, quantity: {}]",
                userId, portfolioId, request.getShareId().getIsin(), request.getShareId().getTickerSymbol(), request.getQuantity());
        }
        return Response.ok(marshal(holding)).build();
    }

    private PortfolioSummaryResponse marshal(Portfolio portfolio) {
        return new PortfolioSummaryResponse()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .dateCreated(portfolio.getDateCreated())
            .holdingCount(portfolio.getHoldingCount());
    }

    private PortfolioResponse marshalDetail(Portfolio portfolio) {
        return new PortfolioResponse()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .dateCreated(portfolio.getDateCreated())
            .holdingCount(portfolio.getHoldingCount())
            .holdings(portfolio.getHoldings().stream()
                .map(this::marshal).toList()
            );
    }

    private HoldingResponse marshal(Holding holding) {
        ShareIndex shareIndex = holding.getShareIndex();
        BigDecimal mostRecentPrice = sharePriceService.getMostRecentPrice(shareIndex)
            .map(PriceHistory::getClose)
            .orElse(BigDecimal.ZERO);

        int totalQuantity = holding.getQuantity();
        Double totalValue = mostRecentPrice.multiply(BigDecimal.valueOf(totalQuantity)).doubleValue();

        return new HoldingResponse()
            .id(holding.getId())
            .shareIndexId(shareIndex.getId())
            .shareId(new ShareId()
                .isin(shareIndex.getIdentity().getIsin())
                .tickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            )
            .name(shareIndex.getName())
            .totalCost(holding.getTotalCost().doubleValue())
            .currency(holding.getCurrency().getCurrencyCode())
            .quantity(totalQuantity)
            .latestValue(totalValue)
            .dealings(holding.getDealings().stream()
                .map(this::marshal).toList()
            );
    }

    private DealingHistoryResponse marshal(DealingHistory dealing) {
        return new DealingHistoryResponse()
            .id(dealing.getId())
            .dateExecuted(dealing.getDateExecuted())
            .quantity(dealing.getQuantity())
            .pricePerShare(dealing.getPrice().doubleValue());
    }
}
