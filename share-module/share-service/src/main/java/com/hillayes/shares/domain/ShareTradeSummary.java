package com.hillayes.shares.domain;

import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Getter
public class ShareTradeSummary {
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID portfolioId;

    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID shareIndexId;

    private ShareIndex.ShareIdentity shareIdentity;

    @ToString.Include
    private String name;

    private long quantity;

    private BigDecimal totalCost;

    private Currency currency;

    private BigDecimal latestPrice;
}
