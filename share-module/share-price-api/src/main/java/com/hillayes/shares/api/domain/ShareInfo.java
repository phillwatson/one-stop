package com.hillayes.shares.api.domain;

import lombok.Getter;

import java.util.Currency;

/**
 * Encapsulates the information about a Share or Fund obtained from a ShareProvider.
 *
 * One or both of the ISIN or Ticker Symbol are required for any successful identification.
 *
 */
@Getter
public class ShareInfo {
    private final String isin;
    private final String tickerSymbol;
    private final String name;
    private final Currency currency;

    /**
     * @param isin the International Share Identification Number
     * @param tickerSymbol the stock exchange ticker symbol.
     * @param name the name as registered with the stock exchange.
     * @param currencyCode the currency in which shares are traded.
     */
    public ShareInfo(
        String isin,
        String tickerSymbol,
        String name,
        String currencyCode
    ) {
        this(isin, tickerSymbol, name,
            Currency.getInstance("GBX".equals(currencyCode) ? "GBP" : currencyCode)
        );
    }

    /**
     * @param isin the International Share Identification Number
     * @param tickerSymbol the stock exchange ticker symbol.
     * @param name the name as registered with the stock exchange.
     * @param currency the currency in which shares are traded.
     */
    public ShareInfo(
        String isin,
        String tickerSymbol,
        String name,
        Currency currency
    ) {
        this.isin = isin;
        this.tickerSymbol = tickerSymbol;
        this.name = name;
        this.currency = currency;
    }
}
