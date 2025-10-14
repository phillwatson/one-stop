package com.hillayes.shares.utils;

import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.DealingHistory;
import com.hillayes.shares.domain.ShareHolding;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceHistory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

public class TestData {
    private static final RandomStringUtils randomStrings = RandomStringUtils.insecure();
    private static final RandomUtils randomNumbers = RandomUtils.insecure();

    public static ShareIndex mockShareIndex(UUID id) {
        return ShareIndex.builder()
            .id(id)
            .isin(randomStrings.nextAlphanumeric(12))
            .name(randomStrings.nextAlphanumeric(30))
            .currency(Currency.getInstance("GBP"))
            .provider(ShareProvider.FT_MARKET_DATA)
            .build();
    }

    public static SharePriceHistory mockSharePriceHistory(ShareIndex shareIndex, LocalDate date) {
        return SharePriceHistory.builder()
            .id(SharePriceHistory.PrimaryKey.builder()
                .shareIndexId(shareIndex.getId())
                .date(date)
                .build())
            .open(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .low(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .high(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .close(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .build();
    }

    public static ShareHolding mockShareHolding(UUID id, ShareIndex shareIndex, UUID userId) {
        return ShareHolding.builder()
            .id(id)
            .shareIndexId(shareIndex.getId())
            .userId(userId)
            .dateCreated(Instant.now().minus(Duration.ofDays(90)))
            .build();
    }

    public static DealingHistory mockDealingHistory(UUID id, ShareHolding holding, LocalDate marketDate) {
        int quantity = randomNumbers.randomInt(-100, 100);
        return DealingHistory.builder()
            .id(id)
            .shareHoldingId(holding.getId())
            .marketDate(marketDate)
            .price(BigDecimal.valueOf(randomNumbers.randomFloat(0, 20000)))
            .quantity(quantity)
            .purchase(quantity > 0)
            .build();
    }
}
