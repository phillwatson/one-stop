package com.hillayes.shares.service;

import com.hillayes.shares.domain.ShareDealing;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.errors.SaleExceedsHoldingException;
import com.hillayes.shares.errors.ZeroTradeQuantityException;
import com.hillayes.shares.event.PortfolioEventSender;
import com.hillayes.shares.repository.HoldingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShareTradeService {
    private final HoldingRepository holdingRepository;
    private final PortfolioEventSender portfolioEventSender;

    /**
     * Registers a new trade in the identified share to be held within the
     * identified user's portfolio.
     *
     * @param portfolio the portfolio to which the trade is to be recorded.
     * @param dateExecuted the date on which the trade was executed.
     * @param shareIndex the stock being traded.
     * @param quantity the number of shares traded (negative = sell, positive = buy)
     * @param pricePerShare the price at which the shares were traded.
     * @return the updated holding.
     */
    public Holding recordShareTrade(Portfolio portfolio,
                                    ShareIndex shareIndex,
                                    LocalDate dateExecuted,
                                    int quantity,
                                    BigDecimal pricePerShare) {
        log.info("Creating a share trade [portfolio: {}, date: {}, shareIndexId: {}, quantity: {}, price: {}]",
            portfolio.getName(), dateExecuted, shareIndex.getName(), quantity, pricePerShare);

        if (quantity == 0) {
            throw new ZeroTradeQuantityException(shareIndex);
        }

        Holding holding = holdingRepository.getHoldings(portfolio.getId(), shareIndex.getId())
            .orElseGet(() -> Holding.builder()
                .portfolioId(portfolio.getId())
                .shareIndex(shareIndex)
                .build()
            );

        return recordShareTrade(holding, shareIndex, dateExecuted, quantity, pricePerShare);
    }

    public Holding recordShareTrade(Holding holding,
                                    ShareIndex shareIndex,
                                    LocalDate dateExecuted,
                                    int quantity,
                                    BigDecimal pricePerShare) {
        log.info("Creating a share trade [holdingId: {}, date: {}, quantity: {}, price: {}]",
            holding.getId(), dateExecuted, quantity, pricePerShare);

        if (quantity == 0) {
            throw new ZeroTradeQuantityException(shareIndex);
        }

        ShareDealing dealing;
        if (quantity > 0)
            dealing = holding.buy(dateExecuted, quantity, pricePerShare);
        else if (-quantity > holding.getQuantity())
            throw new SaleExceedsHoldingException(shareIndex, quantity, holding.getQuantity());
        else
            dealing = holding.sell(dateExecuted, quantity, pricePerShare);

        holdingRepository.saveAndFlush(holding);
        portfolioEventSender.sendSharesTransacted(dealing);
        return holding;
    }
}
