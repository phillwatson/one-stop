package com.hillayes.shares.resource;

import com.hillayes.onestop.api.HoldingResponse;
import com.hillayes.onestop.api.PortfolioResponse;
import com.hillayes.onestop.api.ShareDealingResponse;
import com.hillayes.onestop.api.ShareId;
import com.hillayes.shares.domain.*;
import com.hillayes.shares.service.SharePriceService;

import java.math.BigDecimal;

public class ResourceBase {
    private final SharePriceService sharePriceService;

    ResourceBase(SharePriceService sharePriceService) {
        this.sharePriceService = sharePriceService;
    }

    protected PortfolioResponse marshal(Portfolio portfolio) {
        return new PortfolioResponse()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .dateCreated(portfolio.getDateCreated())
            .holdingCount(portfolio.getHoldingCount());
    }

    protected HoldingResponse marshal(Holding holding) {
        ShareIndex shareIndex = holding.getShareIndex();
        BigDecimal mostRecentPrice = sharePriceService.getMostRecentPrice(shareIndex)
            .map(PriceHistory::getClose)
            .orElse(BigDecimal.ZERO);

        int totalQuantity = holding.getQuantity();
        Double totalValue = mostRecentPrice.multiply(BigDecimal.valueOf(totalQuantity)).doubleValue();

        return new HoldingResponse()
            .id(holding.getId())
            .portfolioId(holding.getPortfolioId())
            .shareIndexId(shareIndex.getId())
            .shareId(new ShareId()
                .isin(shareIndex.getIdentity().getIsin())
                .tickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            )
            .name(shareIndex.getName())
            .totalCost(holding.getTotalCost().doubleValue())
            .currency(holding.getCurrency().getCurrencyCode())
            .quantity(totalQuantity)
            .latestValue(totalValue);
    }

    protected ShareDealingResponse marshal(ShareDealing dealing) {
        return new ShareDealingResponse()
            .id(dealing.getId())
            .shareIndexId(dealing.getHolding().getShareIndex().getId())
            .quantity(dealing.getQuantity())
            .pricePerShare(dealing.getPrice().doubleValue())
            .dateExecuted(dealing.getDateExecuted());
    }
}
