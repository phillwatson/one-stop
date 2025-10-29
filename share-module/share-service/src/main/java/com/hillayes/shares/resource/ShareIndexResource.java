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
import jakarta.transaction.Transactional;
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

@Path("/api/v1/shares/indices")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class ShareIndexResource {
    private final ShareIndexService shareIndexService;
    private final SharePriceService sharePriceService;

    @POST
    @Transactional
    public Response registerShareIndices(@Valid List<RegisterShareIndexRequest> request) {
        log.info("Registering share indices [size: {}]", (request == null) ? 0 : request.size());

        if ((request == null) || (request.isEmpty())) {
            return Response.ok(List.of()).build();
        }

        List<ShareIndex> indices = request.stream()
            .map(index -> ShareIndex.builder()
                .isin(index.getIsin())
                .name(index.getName())
                .currency(Currency.getInstance(index.getCurrency()))
                .provider(ShareProvider.valueOf(index.getProvider()))
                .build()
            ).toList();

        List<ShareIndexResponse> result = shareIndexService.registerShareIndices(indices).stream()
            .map(this::marshal)
            .toList();

        log.debug("Registered share indices [size: {}]", result.size());
        return Response.ok(result).build();
    }

    @GET
    public Response getAllShareIndices(@Context UriInfo uriInfo,
                                       @QueryParam("page") @DefaultValue("0") int page,
                                       @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing share indices [page: {}, pageSize: {}]", page, pageSize);

        Page<ShareIndex> sharesPage = shareIndexService.listShareIndices(page, pageSize);

        PaginatedShareIndices response = new PaginatedShareIndices()
            .page(sharesPage.getPageIndex())
            .pageSize(sharesPage.getPageSize())
            .count(sharesPage.getContentSize())
            .total(sharesPage.getTotalCount())
            .totalPages(sharesPage.getTotalPages())
            .items(sharesPage.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, sharesPage));

        log.debug("Listing share indices [page: {}, pageSize: {}, count: {}, total: {}]",
            page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{shareId}/prices")
    public Response getSharePrices(@Context UriInfo uriInfo,
                                   @PathParam("shareId") UUID shareIndexId,
                                   @QueryParam("from-date") LocalDate fromDate,
                                   @QueryParam("to-date") LocalDate toDate,
                                   @QueryParam("page")@DefaultValue("0") int pageIndex,
                                   @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing share prices [shareIndex: {}, fromDate: {}, toDate: {}, page: {}, pageSize: {}",
            shareIndexId, fromDate, toDate, pageIndex, pageSize);

        ShareIndex shareIndex = shareIndexService.getShareIndex(shareIndexId)
            .orElseThrow(() -> new NotFoundException("ShareIndex", shareIndexId));

        Page<PriceHistory> prices = sharePriceService.getPrices(shareIndex, fromDate, toDate, pageIndex, pageSize);

        PaginatedSharePrices response = new PaginatedSharePrices()
            .page(prices.getPageIndex())
            .pageSize(prices.getPageSize())
            .count(prices.getContentSize())
            .total(prices.getTotalCount())
            .totalPages(prices.getTotalPages())
            .items(prices.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, prices));

        log.debug("Listing share prices [page: {}, pageSize: {}, count: {}, total: {}]",
            pageIndex, pageSize, response.getCount(), response.getTotal());
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

    private HistoricalPriceResponse marshal(PriceHistory priceHistory) {
        return new HistoricalPriceResponse()
            .date(priceHistory.getId().getDate())
            .open(priceHistory.getOpen().doubleValue())
            .high(priceHistory.getHigh().doubleValue())
            .low(priceHistory.getLow().doubleValue())
            .close(priceHistory.getClose().doubleValue())
            .volume(priceHistory.getVolume());
    }
}
