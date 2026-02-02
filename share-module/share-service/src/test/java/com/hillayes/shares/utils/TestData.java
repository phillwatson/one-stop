package com.hillayes.shares.utils;

import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class TestData {
    public static final RandomStringUtils randomStrings = RandomStringUtils.insecure();
    public static final RandomUtils randomNumbers = RandomUtils.insecure();

    public static ShareIndex.ShareIdentity mockShareIdentity() {
        return mockShareIdentity(null);
    }

    public static ShareIndex.ShareIdentity mockShareIdentity(Consumer<ShareIndex.ShareIdentity.ShareIdentityBuilder> modifier) {
        ShareIndex.ShareIdentity.ShareIdentityBuilder builder = ShareIndex.ShareIdentity.builder()
            .isin(randomStrings.nextAlphanumeric(12))
            .tickerSymbol(randomStrings.nextAlphabetic(4));

        if (modifier != null) {
            modifier.accept(builder);
        }

        return builder.build();
    }

    public static ShareIndex mockShareIndex() {
        return mockShareIndex(null);
    }

    public static ShareIndex mockShareIndex(Consumer<ShareIndex.Builder> modifier) {
        ShareIndex.Builder builder = ShareIndex.builder()
            .identity(mockShareIdentity(null))
            .name(randomStrings.nextAlphanumeric(30))
            .currency(Currency.getInstance("GBP"))
            .provider(ShareProvider.FT_MARKET_DATA);

        if (modifier != null) {
            modifier.accept(builder);
        }

        return builder.build();
    }

    public static PriceHistory mockPriceHistory(ShareIndex shareIndex, LocalDate date, SharePriceResolution resolution) {
        return mockPriceHistory(shareIndex, date, resolution, null);
    }

    public static PriceHistory mockPriceHistory(ShareIndex shareIndex, LocalDate date, SharePriceResolution resolution,
                                                Consumer<PriceHistory.Builder> modifier) {
        PriceHistory.Builder builder = PriceHistory.builder()
            .id(PriceHistory.PrimaryKey.builder()
                .shareIndexId(shareIndex.getId())
                .resolution(resolution)
                .date(date)
                .build())
            .open(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .low(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .high(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .close(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)));

        if (modifier != null) {
            modifier.accept(builder);
        }

        return builder.build();
    }

    public static List<PriceHistory> mockPriceHistory(ShareIndex shareIndex, LocalDate fromDate, LocalDate toDate) {
        List<PriceHistory> result = new ArrayList<>();
        LocalDate date = fromDate;
        while (!date.isAfter(toDate)) {
            result.add(mockPriceHistory(shareIndex, date, SharePriceResolution.DAILY));
            date = date.plusDays(1);
        }

        return result;
    }

    public static Portfolio mockPortfolio(UUID userId) {
        return mockPortfolio(userId, null);
    }

    public static Portfolio mockPortfolio(UUID userId,
                                          Consumer<Portfolio.Builder> modifier) {
        Portfolio.Builder builder = Portfolio.builder()
            .userId(userId)
            .name(randomStrings.nextAlphanumeric(30))
            .dateCreated(Instant.now().minus(Duration.ofDays(90)));

        if (modifier != null) {
            modifier.accept(builder);
        }

        return builder.build();
    }

    public static ShareTrade mockShareTrade(Portfolio portfolio, ShareIndex shareIndex) {
        return mockShareTrade(portfolio, shareIndex, null);
    }

    public static ShareTrade mockShareTrade(Portfolio portfolio, ShareIndex shareIndex,
                                            Consumer<ShareTrade.Builder> modifier) {
        ShareTrade.Builder builder = ShareTrade.builder()
            .userId(portfolio.getUserId())
            .portfolioId(portfolio.getId())
            .shareIndexId(shareIndex.getId())
            .dateExecuted(LocalDate.now().minusDays(randomNumbers.randomInt(1, 40)))
            .quantity(randomNumbers.randomInt())
            .price(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)));

        if (modifier != null) {
            modifier.accept(builder);
        }

        return builder.build();
    }

    public static PriceData mockPriceData(LocalDate date) {
        return new PriceData(
            date,
            BigDecimal.valueOf(randomNumbers.randomDouble(10.00, 20.00)),
            BigDecimal.valueOf(randomNumbers.randomDouble(10.00, 20.00)),
            BigDecimal.valueOf(randomNumbers.randomDouble(10.00, 20.00)),
            BigDecimal.valueOf(randomNumbers.randomDouble(10.00, 20.00)),
            randomNumbers.randomLong(100, 2000)
        );
    }
}
