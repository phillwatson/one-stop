package com.hillayes.shares.event;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.portfolio.SharesTransacted;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.shares.domain.DealingHistory;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PortfolioEventSender {
    private final EventSender eventSender;

    public void sendSharesTransacted(DealingHistory purchase) {
        Portfolio portfolio = purchase.getHolding().getPortfolio();
        ShareIndex shareIndex = purchase.getHolding().getShareIndex();
        log.debug("Sending SharesTransacted event [dealingId: {}, userId: {}, company: {}]",
            purchase.getId(), portfolio.getUserId(), shareIndex.getName());

        eventSender.send(Topic.PORTFOLIO, SharesTransacted.builder()
            .userId(portfolio.getUserId())
            .portfolioId(portfolio.getId())
            .portfolioName(portfolio.getName())
            .companyIsin(shareIndex.getIdentity().getIsin())
            .companyTickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            .companyName(shareIndex.getName())
            .dateExecuted(purchase.getDateExecuted())
            .purchase(purchase.getQuantity() > 0)
            .quantity(Math.abs(purchase.getQuantity()))
            .price(purchase.getPrice())
            .currency(shareIndex.getCurrency().getCurrencyCode())
            .build());
    }
}
