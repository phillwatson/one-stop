package com.hillayes.shares.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.service.ShareIndexService;
import com.hillayes.shares.service.SharePriceService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/shares/indexes")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class ShareIndexResource {
    private final ShareIndexService shareIndexService;
    private final SharePriceService sharePriceService;

    @POST
    public Response registerShareIndexes(@Valid List<RegisterShareIndexRequest> request) {
        log.info("Registering share indexes [size: {}]", request.size());

        List<ShareIndexResponse> result = request.stream()
            .map(index ->
                shareIndexService.registerShareIndex(index.getIsin(), index.getName(),
                    Currency.getInstance(index.getCurrency()),
                    ShareProvider.valueOf(index.getProvider())))
            .map(this::marshal)
            .toList();

        log.debug("Registered share indexes [size: {}]", result.size());
        return Response.ok(result).build();
    }

    @GET
    public Response getAllShareIndexes(@Context UriInfo uriInfo,
                                       @QueryParam("page") @DefaultValue("0") int page,
                                       @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing share indexes [page: {}, pageSize: {}]", page, pageSize);

        Page<ShareIndex> sharesPage = shareIndexService.listShareIndexes(page, pageSize);

        PaginatedShareIndexes response = new PaginatedShareIndexes()
            .page(sharesPage.getPageIndex())
            .pageSize(sharesPage.getPageSize())
            .count(sharesPage.getContentSize())
            .total(sharesPage.getTotalCount())
            .totalPages(sharesPage.getTotalPages())
            .items(sharesPage.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, sharesPage));

        log.debug("Listing share indexes [page: {}, pageSize: {}, count: {}, total: {}]",
            page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{shareId}/prices")
    public Response getSharePrices(@Context UriInfo uriInfo,
                                   @PathParam("shareId") UUID shareIndexId,
                                   @QueryParam("from-date") LocalDate fromDate,
                                   @QueryParam("to-date") LocalDate toDate,
                                   @QueryParam("page")@DefaultValue("0") int page,
                                   @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing share prices [shareIndex: {}, fromDate: {}, toDate: {}, page: {}, pageSize: {}",
            shareIndexId, fromDate, toDate, page, pageSize);

        ShareIndex shareIndex = shareIndexService.getShareIndex(shareIndexId)
            .orElseThrow(() -> new NotFoundException("share-index", shareIndexId));

        Page<PriceHistory> prices = sharePriceService.getPrices(shareIndex, fromDate, toDate, page, pageSize);

        PaginatedSharePrices response = new PaginatedSharePrices()
            .page(prices.getPageIndex())
            .pageSize(prices.getPageSize())
            .count(prices.getContentSize())
            .total(prices.getTotalCount())
            .totalPages(prices.getTotalPages())
            .items(prices.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, prices));

        log.debug("Listing share prices [page: {}, pageSize: {}, count: {}, total: {}]",
            page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    private ShareIndexResponse marshal(ShareIndex shareIndex) {
        return new ShareIndexResponse()
            .id(shareIndex.getId())
            .isin(shareIndex.getIsin())
            .name(shareIndex.getName())
            .currency(shareIndex.getCurrency().getCurrencyCode())
            .provider(shareIndex.getProvider().name());
    }

    private HistoricalPrice marshal(PriceHistory priceHistory) {
        return new HistoricalPrice()
            .date(priceHistory.getId().getDate())
            .open(priceHistory.getOpen().doubleValue())
            .high(priceHistory.getHigh().doubleValue())
            .low(priceHistory.getLow().doubleValue())
            .close(priceHistory.getClose().doubleValue())
            .volume(priceHistory.getVolume());
    }
}
