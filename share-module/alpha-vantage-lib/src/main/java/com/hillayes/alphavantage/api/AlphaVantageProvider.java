package com.hillayes.alphavantage.api;

import com.hillayes.alphavantage.api.domain.ApiFunction;
import com.hillayes.alphavantage.api.domain.DailyTimeSeries;
import com.hillayes.alphavantage.api.domain.TickerSearchRecord;
import com.hillayes.alphavantage.api.domain.TickerSearchResponse;
import com.hillayes.alphavantage.api.service.AlphaVantageApi;
import com.hillayes.commons.Strings;
import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareInfo;
import com.hillayes.shares.api.domain.ShareProvider;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * The bridge that provides access to the Alpha Vantage share price data.
 */
@ApplicationScoped
//@RequiredArgsConstructor
@Slf4j
public class AlphaVantageProvider implements ShareProviderApi {
    private final String API_KEY;
    private final AlphaVantageApi alphaVantageApi;

    AlphaVantageProvider(@ConfigProperty(name = "one-stop.alpha-vantage-api.secret-key", defaultValue = "not-set")
                         String API_KEY,
                         AlphaVantageApi alphaVantageApi) {
        this.API_KEY = API_KEY;
        this.alphaVantageApi = alphaVantageApi;
    }

    @Override
    public ShareProvider getProviderId() {
        return ShareProvider.ALPHA_ADVANTAGE;
    }

    @Override
    public int getMaxHistory() {
        return 100;
    }

    @Override
    public Optional<ShareInfo> getShareInfo(String isin, String tickerSymbol) {
        if (Strings.isBlank(tickerSymbol)) {
            // we can only identify shares on their Ticker Symbol
            return Optional.empty();
        }

        TickerSearchResponse response = alphaVantageApi.symbolSearch(API_KEY, ApiFunction.SYMBOL_SEARCH, tickerSymbol);

        if ((response == null) || (response.bestMatches == null)) {
            return Optional.empty();
        }

        if (response.bestMatches.isEmpty()) {
            // give it another try without .LSE suffix
            if (tickerSymbol.endsWith(".LSE")) {
                return getShareInfo(isin, tickerSymbol.substring(0, tickerSymbol.length() - 4) + ".LON");
            }
        }

        TickerSearchRecord record = (response.bestMatches.size() > 1)
            // find the first one with ".LON" suffix
            ? response.bestMatches.stream()
                .filter(r -> r.symbol.equals(tickerSymbol + ".LON"))
                .findFirst()
                .orElse(null)
            : response.bestMatches.isEmpty() ? null : response.bestMatches.get(0);

        return record == null ? Optional.empty() :
            Optional.of(new ShareInfo(
                    isin,
                    record.symbol,
                    record.name,
                    Currency.getInstance("GBX".equals(record.currency) ? "GBP" : record.currency)
                )
            );
    }

    @Override
    public Optional<List<PriceData>> getPrices(String stockIsin, String tickerSymbol,
                                               LocalDate startDate, LocalDate endDate) {
        if (Strings.isBlank(tickerSymbol)) {
            // we can only identify shares on their Ticker Symbol
            return Optional.empty();
        }

        DailyTimeSeries series = alphaVantageApi.getDailySeries(API_KEY, ApiFunction.TIME_SERIES_DAILY, tickerSymbol);
        if ((series == null) || (series.series == null) || (series.series.isEmpty())) {
            return Optional.empty();
        }

        return Optional.of(series.series.entrySet().stream()
            .filter(entry -> (!entry.getKey().isBefore(startDate)) && (!entry.getKey().isAfter(endDate)))
            .map(entry -> new PriceData(entry.getKey(),
                BigDecimal.valueOf(entry.getValue().open),
                BigDecimal.valueOf(entry.getValue().high),
                BigDecimal.valueOf(entry.getValue().low),
                BigDecimal.valueOf(entry.getValue().close),
                Long.valueOf(entry.getValue().volume)
            ))
            .sorted(Comparator.comparing(PriceData::date))
            .toList()
        );
    }
}
