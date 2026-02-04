package com.hillayes.shares.event;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.portfolio.ShareTradeDeleted;
import com.hillayes.events.events.portfolio.ShareTradeUpdated;
import com.hillayes.events.events.portfolio.SharesTransacted;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareTrade;
import io.micrometer.core.annotation.Counted;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PortfolioEventSender {
    private final EventSender eventSender;

    @Counted("portfolio.events")
    public void sendSharesTransacted(Portfolio portfolio,
                                     ShareIndex shareIndex,
                                     ShareTrade shareTrade) {
        log.debug("Sending SharesTransacted event [tradeId: {}, userId: {}, company: {}]",
            shareTrade.getId(), portfolio.getUserId(), shareIndex.getName());

        eventSender.send(Topic.PORTFOLIO, SharesTransacted.builder()
            .userId(portfolio.getUserId())
            .portfolioId(portfolio.getId())
            .portfolioName(portfolio.getName())
            .companyIsin(shareIndex.getIdentity().getIsin())
            .companyTickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            .companyName(shareIndex.getName())
            .tradeId(shareTrade.getId())
            .dateExecuted(shareTrade.getDateExecuted())
            .purchase(shareTrade.getQuantity() > 0)
            .quantity(Math.abs(shareTrade.getQuantity()))
            .price(shareTrade.getPrice())
            .currency(shareIndex.getCurrency().getCurrencyCode())
            .build());
    }

    @Counted("portfolio.events")
    public void sendShareTradeUpdated(Portfolio portfolio,
                                      ShareIndex shareIndex,
                                      ShareTrade shareTrade) {
        log.debug("Sending ShareTradeUpdated event [dealingId: {}, userId: {}, company: {}]",
            shareTrade.getId(), portfolio.getUserId(), shareIndex.getName());

        eventSender.send(Topic.PORTFOLIO, ShareTradeUpdated.builder()
            .userId(portfolio.getUserId())
            .portfolioId(portfolio.getId())
            .portfolioName(portfolio.getName())
            .companyIsin(shareIndex.getIdentity().getIsin())
            .companyTickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            .companyName(shareIndex.getName())
            .tradeId(shareTrade.getId())
            .dateExecuted(shareTrade.getDateExecuted())
            .purchase(shareTrade.getQuantity() > 0)
            .quantity(Math.abs(shareTrade.getQuantity()))
            .price(shareTrade.getPrice())
            .currency(shareIndex.getCurrency().getCurrencyCode())
            .build());
    }

    @Counted("portfolio.events")
    public void sendShareTradeDeleted(Portfolio portfolio,
                                      ShareIndex shareIndex,
                                      ShareTrade shareTrade) {
        log.debug("Sending ShareTradeDeleted event [dealingId: {}, userId: {}, company: {}]",
            shareTrade.getId(), portfolio.getUserId(), shareIndex.getName());

        eventSender.send(Topic.PORTFOLIO, ShareTradeDeleted.builder()
            .userId(portfolio.getUserId())
            .portfolioId(portfolio.getId())
            .portfolioName(portfolio.getName())
            .companyIsin(shareIndex.getIdentity().getIsin())
            .companyTickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            .companyName(shareIndex.getName())
            .tradeId(shareTrade.getId())
            .dateExecuted(shareTrade.getDateExecuted())
            .purchase(shareTrade.getQuantity() > 0)
            .quantity(Math.abs(shareTrade.getQuantity()))
            .price(shareTrade.getPrice())
            .currency(shareIndex.getCurrency().getCurrencyCode())
            .build());
    }
}
