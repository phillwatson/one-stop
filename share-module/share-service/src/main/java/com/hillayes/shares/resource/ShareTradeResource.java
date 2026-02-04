package com.hillayes.shares.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.ShareTradeRequest;
import com.hillayes.onestop.api.ShareTradeResponse;
import com.hillayes.shares.domain.ShareTrade;
import com.hillayes.shares.service.ShareTradeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

@Path("/api/v1/shares/trades/{shareTradeId}")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class ShareTradeResource {
    private final ShareTradeService shareTradeService;

    @GET
    public Response getShareTrade(@Context SecurityContext ctx,
                                  @PathParam("shareTradeId") UUID shareTradeId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Get share trade [userId: {}, shareTradeId: {}", userId, shareTradeId);

        ShareTrade shareTrade = shareTradeService.getShareTrade(userId, shareTradeId)
            .orElseThrow(() -> new NotFoundException("ShareTrade", shareTradeId));

        return Response.ok(marshal(shareTrade)).build();
    }

    /**
     * Updates the identified share trade. Intended for correct errors and NOT
     * to record a new trade on the same share.
     *
     * @param ctx the security context from which the user can be identified.
     * @param shareTradeId the ShareTrade identifier.
     * @param request the details of the update.
     * @return the updated ShareTrade.
     */
    @PUT
    public Response updateShareTrade(@Context SecurityContext ctx,
                                     @PathParam("shareTradeId") UUID shareTradeId,
                                     ShareTradeRequest request) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Update share trade [userId: {}, shareTradeId: {}", userId, shareTradeId);

        ShareTrade shareTrade = shareTradeService
            .updateShareTrade(userId, shareTradeId,
                request.getDateExecuted(),
                request.getQuantity(),
                BigDecimal.valueOf(request.getPricePerShare()))
            .orElseThrow(() -> new NotFoundException("ShareTrade", shareTradeId));

        log.debug("Updated share trade [userId: {}, shareTradeId: {}", userId, shareTradeId);
        return Response.ok(shareTrade).build();
    }

    @DELETE
    public Response deleteShareTrade(@Context SecurityContext ctx,
                                     @PathParam("shareTradeId") UUID shareTradeId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting share trade [userId: {}, shareTradeId: {}", userId, shareTradeId);

        shareTradeService.deleteShareTrade(userId, shareTradeId)
            .orElseThrow(() -> new NotFoundException("ShareTrade", shareTradeId));

        log.debug("Deleted share trade [userId: {}, shareTradeId: {}]", userId, shareTradeId);
        return Response.noContent().build();
    }

    private ShareTradeResponse marshal(ShareTrade shareTrade) {
        return new ShareTradeResponse()
            .id(shareTrade.getId())
            .shareIndexId(shareTrade.getShareIndexId())
            .dateExecuted(shareTrade.getDateExecuted())
            .quantity(shareTrade.getQuantity())
            .pricePerShare(shareTrade.getPrice().doubleValue());
    }
}
