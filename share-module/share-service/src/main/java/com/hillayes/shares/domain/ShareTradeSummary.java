package com.hillayes.shares.domain;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
public class ShareTradeSummary {
    private UUID portfolioId;

    private UUID shareIndexId;

    private ShareIndex.ShareIdentity shareIdentity;

    private String tickerSymbol;

    private String name;

    private long quantity;

    private BigDecimal totalCost;

    private Currency currency;

    private BigDecimal latestPrice;
}
