package com.hillayes.events.events.portfolio;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class ShareTradeDeleted {
    /**
     * The ID of the user that has made the transaction.
     */
    private UUID userId;

    /**
     * The ID of the user's portfolio in which the transaction was made.
     */
    private UUID portfolioId;

    /**
     * The name of the user's portfolio in which the transaction was made.
     */
    private String portfolioName;

    /**
     * The ISIN of the company in which the transaction was made. May be null.
     */
    private String companyIsin;

    /**
     * The ticker symbol of the company in which the transaction was made. May be null.
     */
    private String companyTickerSymbol;

    /**
     * The name of the company in which the transaction was made.
     */
    private String companyName;

    /**
     * The internal identifier of the share trade. This can be used to correlate
     * the updated trade record with its original.
     */
    private UUID tradeId;

    /**
     * The date on which the transaction was made.
     */
    private LocalDate dateExecuted;

    /**
     * True if the transaction was for a purchase. False for a sale.
     */
    private boolean purchase;

    /**
     * The number of shares bought or sold. Always positive
     */
    private int quantity;

    /**
     * The price paid or received for each share.
     */
    private BigDecimal price;

    /**
     * The ISO code for the currency in which the shares a transacted.
     */
    private String currency;
}
