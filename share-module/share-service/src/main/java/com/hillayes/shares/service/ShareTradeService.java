package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.*;
import com.hillayes.shares.errors.ZeroTradeQuantityException;
import com.hillayes.shares.event.PortfolioEventSender;
import com.hillayes.shares.repository.PortfolioRepository;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.repository.ShareTradeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShareTradeService {
    private final ShareTradeRepository shareTradeRepository;
    private final ShareIndexRepository shareIndexRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioEventSender portfolioEventSender;

    public Optional<ShareTrade> getShareTrade(UUID userId, UUID shareTradeId) {
        log.info("get shareTrade [userId: {}, shareTradeId: {}]", userId, shareTradeId);

        return shareTradeRepository.findByIdOptional(shareTradeId)
            .filter(shareTrade -> shareTrade.getUserId().equals(userId));
    }

    public List<ShareTradeSummary> getShareTradeSummaries(Portfolio portfolio) {
        log.info("Get share trade summaries [portfolio: {}]", portfolio);
        return shareTradeRepository
            .getShareTradeSummaries(portfolio.getUserId(), portfolio.getId()).stream()
            .map(summary -> ShareTradeSummary.builder()
                .portfolioId(summary.getPortfolioId())
                .shareIndexId(summary.getShareIndexId())
                .shareIdentity(ShareIndex.ShareIdentity.builder()
                    .isin(summary.getIsin())
                    .tickerSymbol(summary.getTickerSymbol())
                    .build())
                .name(summary.getName())
                .quantity(summary.getQuantity())
                .currency(Currency.getInstance(summary.getCurrency()))
                .totalCost(summary.getTotalCost())
                .latestPrice(priceHistoryRepository.getMostRecent(summary.getShareIndexId())
                    .map(PriceHistory::getClose)
                    .orElse(BigDecimal.ZERO))
                .build())
            .toList();
    }

    public Page<ShareTrade> getShareTrades(Portfolio portfolio,
                                           ShareIndex shareIndex,
                                           int pageIndex,
                                           int pageSize) {
        log.info("Listing user's portfolios [userId: {}, portfolioId: {}, shareIndexId: {}, page: {}, pageSize: {}]",
            portfolio.getUserId(), portfolio.getId(), shareIndex.getId(), pageIndex, pageSize);
        return shareTradeRepository.getShareTrades(portfolio, shareIndex, pageIndex, pageSize);
    }

    /**
     * Registers a new trade in the identified share to be held within the
     * identified user's portfolio.
     *
     * @param portfolio     the portfolio to which the trade is to be recorded.
     * @param dateExecuted  the date on which the trade was executed.
     * @param shareIndex    the stock being traded.
     * @param quantity      the number of shares traded (negative = sell, positive = buy)
     * @param pricePerShare the price at which the shares were traded.
     * @return the new share trade record.
     */
    public ShareTrade recordShareTrade(Portfolio portfolio,
                                       ShareIndex shareIndex,
                                       LocalDate dateExecuted,
                                       BigDecimal quantity,
                                       BigDecimal pricePerShare) {
        log.info("Creating a share trade [portfolio: {}, date: {}, shareIndexId: {}, quantity: {}, price: {}]",
            portfolio.getName(), dateExecuted, shareIndex.getId(), quantity, pricePerShare);

        if (BigDecimal.ZERO.equals(quantity)) {
            throw new ZeroTradeQuantityException(shareIndex);
        }

        ShareTrade shareTrade = shareTradeRepository.saveAndFlush(
            ShareTrade.builder()
                .userId(portfolio.getUserId())
                .portfolioId(portfolio.getId())
                .shareIndexId(shareIndex.getId())
                .quantity(quantity)
                .price(pricePerShare)
                .dateExecuted(dateExecuted)
                .build()
        );

        portfolioEventSender.sendSharesTransacted(portfolio, shareIndex, shareTrade);
        return shareTrade;
    }

    /**
     * Updates the identified share trade. Intended for correct errors and NOT
     * to record a new trade on the same share.
     *
     * @param userId        the ID of the user making the update.
     * @param shareTradeId  the ID of the share trade to be updated.
     * @param dateExecuted  the date on which the trade was executed.
     * @param quantity      the number of shares traded (negative = sell, positive = buy)
     * @param pricePerShare the price at which the shares were traded.
     * @return the updated holding.
     */
    public Optional<ShareTrade> updateShareTrade(UUID userId,
                                                 UUID shareTradeId,
                                                 LocalDate dateExecuted,
                                                 BigDecimal quantity,
                                                 BigDecimal pricePerShare) {
        log.info("Creating a share trade [shareTradeId: {}, date: {}, quantity: {}, price: {}]",
            shareTradeId, dateExecuted, quantity, pricePerShare);

        ShareTrade shareTrade = getShareTrade(userId, shareTradeId)
            .orElse(null);
        if (shareTrade == null) {
            return Optional.empty();
        }

        ShareIndex shareIndex = shareIndexRepository.findById(shareTrade.getShareIndexId());
        if (BigDecimal.ZERO.equals(quantity)) {
            throw new ZeroTradeQuantityException(shareIndex);
        }

        shareTrade.setQuantity(quantity);
        shareTrade.setPrice(pricePerShare);
        shareTrade.setDateExecuted(dateExecuted);
        shareTrade = shareTradeRepository.saveAndFlush(shareTrade);

        Portfolio portfolio = portfolioRepository.findById(shareTrade.getPortfolioId());
        portfolioEventSender.sendShareTradeUpdated(portfolio, shareIndex, shareTrade);
        return Optional.of(shareTrade);
    }

    public Optional<ShareTrade> deleteShareTrade(UUID userId, UUID shareTradeId) {
        log.info("Deleting portfolio [shareTradeId: {}]", shareTradeId);
        return shareTradeRepository.findByIdOptional(shareTradeId)
            .filter(shareTrade -> userId.equals(shareTrade.getUserId()))
            .map(shareTrade -> {
                shareTradeRepository.delete(shareTrade);

                log.debug("Deleted share trade [id: {}]", shareTrade.getId());

                Portfolio portfolio = portfolioRepository.findById(shareTrade.getPortfolioId());
                ShareIndex shareIndex = shareIndexRepository.findById(shareTrade.getShareIndexId());
                portfolioEventSender.sendShareTradeDeleted(portfolio, shareIndex, shareTrade);
                return shareTrade;
            });
    }
}
