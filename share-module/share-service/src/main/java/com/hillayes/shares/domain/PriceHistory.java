package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "shares", name = "price_history")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PriceHistory {
    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    private PrimaryKey id;

    /**
     * The opening price on the date.
     */
    @Column(name = "open_price", nullable = false)
    private BigDecimal open;

    /**
     * The highest price achieved on the date.
     */
    @Column(name = "high_price", nullable = false)
    private BigDecimal high;

    /**
     * The lowest price achieved on the date.
     */
    @Column(name = "low_price", nullable = false)
    private BigDecimal low;

    /**
     * The price at market close on the date.
     */
    @ToString.Include
    @Column(name = "close_price", nullable = false)
    private BigDecimal close;

    @ToString.Include
    @Column(name = "volume", nullable = false)
    private long volume;

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class PrimaryKey implements Serializable {
        /**
         * The Share/Fund to which this record belongs.
         */
        @Column(name = "share_index_id", nullable = false)
        private UUID shareIndexId;

        @Enumerated(EnumType.STRING)
        @Column(name = "resolution", nullable = false)
        private SharePriceResolution resolution;

        /**
         * The date to which the price data applies.
         */
        @Column(name = "market_date", nullable = false)
        private LocalDate date;
    }
}
