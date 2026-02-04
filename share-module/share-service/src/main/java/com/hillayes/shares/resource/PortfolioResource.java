package com.hillayes.shares.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareTrade;
import com.hillayes.shares.domain.ShareTradeSummary;
import com.hillayes.shares.service.PortfolioService;
import com.hillayes.shares.service.ShareIndexService;
import com.hillayes.shares.service.ShareTradeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/shares/portfolios")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class PortfolioResource {
    private final PortfolioService portfolioService;
    private final ShareIndexService shareIndexService;
    private final ShareTradeService shareTradeService;

    @GET
    public Response getPortfolios(@Context SecurityContext ctx,
                                  @Context UriInfo uriInfo,
                                  @QueryParam("page") @DefaultValue("0") int pageIndex,
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

    @POST
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

    @GET
    @Path("/{portfolioId}")
    public Response getPortfolio(@Context SecurityContext ctx,
                                 @PathParam("portfolioId") UUID portfolioId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);

        Portfolio portfolio = portfolioService.getPortfolio(userId, portfolioId)
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        log.debug("Retrieved portfolio [userId: {}, portfolioId: {}]", userId, portfolioId);
        return Response.ok(marshal(portfolio)).build();
    }

    @PUT
    @Path("/{portfolioId}")
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

    /**
     * Returns a summary list of the share trades within the user's identified portfolio
     * in ascending order of share index name.
     * @param ctx the security context from which the user can be identified.
     * @param portfolioId the portfolio identifier.
     * @return the summary list of share trades.
     */
    @GET
    @Path("/{portfolioId}/trades")
    public Response getPortfolioHoldings(@Context SecurityContext ctx,
                                         @PathParam("portfolioId") UUID portfolioId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Listing portfolio's trades [userId: {}, portfolioId: {}]", userId, portfolioId);

        Portfolio portfolio = portfolioService.getPortfolio(userId, portfolioId)
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        List<ShareTradeSummaryResponse> response = shareTradeService.getShareTradeSummaries(portfolio).stream()
            .map(this::marshal)
            .toList();
        return Response.ok(response).build();
    }

    /**
     * Creates a new share trade record within the user's identified portfolio.
     * @param ctx the security context from which the user can be identified.
     *
     * @param portfolioId the portfolio identifier.
     * @param request the details of the share trade.
     * @return the new share trade record.
     */
    @POST
    @Path("/{portfolioId}/trades")
    public Response createShareTrade(@Context SecurityContext ctx,
                                     @PathParam("portfolioId") UUID portfolioId,
                                     ShareTradeRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Create a share trade [userId: {}, portfolioId: {}, shareIndexId: {}]",
            userId, portfolioId, request.getShareIndexId());

        Portfolio portfolio = portfolioService.getPortfolio(userId, portfolioId)
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        ShareIndex shareIndex = shareIndexService.getShareIndex(request.getShareIndexId())
            .orElseThrow(() -> new NotFoundException("Share Index", request.getShareIndexId()));

        ShareTrade shareTrade = shareTradeService.recordShareTrade(portfolio, shareIndex,
            request.getDateExecuted(),
            request.getQuantity(),
            BigDecimal.valueOf(request.getPricePerShare()));

        return Response.ok(marshal(shareTrade)).build();
    }

    /**
     * Returns a paginated list for the trades of the identified share index within
     * the identified portfolio in ascending date order.
     *
     * @param ctx the security context from which the user can be identified.
     * @param uriInfo the URI info used to construct page links.
     * @param portfolioId the portfolio identifier.
     * @param shareIndexId the share index whose trades are to be returned.
     * @return the paginates list of trades for the identified share index.
     */
    @GET
    @Path("/{portfolioId}/trades/{shareIndexId}")
    public Response getShareTrades(@Context SecurityContext ctx,
                                   @Context UriInfo uriInfo,
                                   @PathParam("portfolioId") UUID portfolioId,
                                   @PathParam("shareIndexId") UUID shareIndexId,
                                   @QueryParam("page") @DefaultValue("0") int pageIndex,
                                   @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Listing share's trades [userId: {}, portfolioId: {}, shareIndexId: {}]",
            userId, portfolioId, shareIndexId);

        Portfolio portfolio = portfolioService.getPortfolio(userId, portfolioId)
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        ShareIndex shareIndex = shareIndexService.getShareIndex(shareIndexId)
            .orElseThrow(() -> new NotFoundException("Share index", shareIndexId));

        Page<ShareTrade> shareTrades = shareTradeService.getShareTrades(portfolio, shareIndex, pageIndex, pageSize);
        PaginatedShareTrades response = new PaginatedShareTrades()
            .page(shareTrades.getPageIndex())
            .pageSize(shareTrades.getPageSize())
            .count(shareTrades.getContentSize())
            .total(shareTrades.getTotalCount())
            .totalPages(shareTrades.getTotalPages())
            .items(shareTrades.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, shareTrades));

        if (log.isDebugEnabled()) {
            log.debug("Listing share trades [userId: {}, portfolioId: {}, shareIndexId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, portfolioId, shareIndexId, pageIndex, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    private PortfolioResponse marshal(Portfolio portfolio) {
        return new PortfolioResponse()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .dateCreated(portfolio.getDateCreated());
    }

    private ShareTradeSummaryResponse marshal(ShareTradeSummary summary) {
        return new ShareTradeSummaryResponse()
            .portfolioId(summary.getPortfolioId())
            .shareIndexId(summary.getShareIndexId())
            .shareId(marshal(summary.getShareIdentity()))
            .name(summary.getName())
            .quantity(summary.getQuantity())
            .totalCost(summary.getTotalCost().doubleValue())
            .currency(summary.getCurrency().getCurrencyCode())
            .latestPrice(summary.getLatestPrice().doubleValue());
    }

    private ShareTradeResponse marshal(ShareTrade shareTrade) {
        return new ShareTradeResponse()
            .id(shareTrade.getId())
            .shareIndexId(shareTrade.getShareIndexId())
            .dateExecuted(shareTrade.getDateExecuted())
            .quantity(shareTrade.getQuantity())
            .pricePerShare(shareTrade.getPrice().doubleValue());
    }

    private ShareId marshal(ShareIndex.ShareIdentity identity) {
        return new ShareId()
            .isin(identity.getIsin())
            .tickerSymbol(identity.getTickerSymbol());
    }
}
