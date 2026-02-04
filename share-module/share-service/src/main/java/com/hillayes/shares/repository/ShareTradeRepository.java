package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareTrade;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ShareTradeRepository extends RepositoryBase<ShareTrade, UUID> {
    private static final String SELECT_TRADE_SUMMARIES =
        "select " +
            "  t.portfolio_id, " +
            "  t.share_index_id, " +
            "  sum(t.quantity) as quantity, " +
            "  sum(t.price * t.quantity) as total_cost " +
            "from shares.share_trade t " +
            "where t.user_id = :userId " +
            "and t.portfolio_id = :portfolioId " +
            "group by t.portfolio_id, t.share_index_id";

    private static final String SELECT_SUMMARIES =
        "select " +
            "  t.portfolio_id, " +
            "  t.share_index_id, " +
            "  s.isin, " +
            "  s.ticker_symbol, " +
            "  s.name, " +
            "  t.quantity, " +
            "  t.total_cost, " +
            "  s.currency_code " +
            "from (" + SELECT_TRADE_SUMMARIES + ") t " +
            "inner join shares.share_index s on t.share_index_id = s.id " +
            "order by s.name";

    public List<ShareTradeSummaryProjection> getShareTradeSummaries(UUID userId,
                                                                    UUID portfolioId) {
        return getEntityManager().createNativeQuery(SELECT_SUMMARIES, ShareTradeSummaryProjection.class)
            .setParameter("userId", userId)
            .setParameter("portfolioId", portfolioId)
            .getResultList();
    }


    /**
     * Returns trades in the given share index for the given portfolio. The
     * trades are returned in ascending dateExecuted order.
     *
     * @param portfolio the portfolios in which the trades are recorded.
     * @param shareIndex the share index that was traded.
     * @param pageIndex the (zero-based) index of the page to be returned
     * @param pageSize the size of the page.
     * @return the qualified sub-set of Portfolio records.
     */
    public Page<ShareTrade> getShareTrades(Portfolio portfolio, ShareIndex shareIndex,
                                           int pageIndex, int pageSize) {
        return pageAll("userId = :userId AND portfolioId = :portfolioId AND shareIndexId = :shareIndexId",
            pageIndex, pageSize,
            OrderBy.by("dateExecuted"),
            Map.of(
                "userId", portfolio.getUserId(),
                "portfolioId", portfolio.getId(),
                "shareIndexId", shareIndex.getId()
            ));
    }

    @Builder
    @AllArgsConstructor
    @Getter
    @RegisterForReflection
    public static class ShareTradeSummaryProjection {
        @Column(name = "portfolio_id")
        private UUID portfolioId;

        @Column(name = "share_index_id")
        private UUID shareIndexId;

        @Column(name = "isin")
        private String isin;

        @Column(name = "ticker_symbol")
        private String tickerSymbol;

        @Column(name = "name")
        private String name;

        @Column(name = "quantity")
        private long quantity;

        @Column(name = "total_cost")
        private BigDecimal totalCost;

        @Column(name = "currency_code")
        private String currency;
    }
}
