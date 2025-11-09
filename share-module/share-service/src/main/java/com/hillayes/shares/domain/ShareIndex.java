package com.hillayes.shares.domain;

import com.hillayes.commons.jpa.CurrencyConverter;
import com.hillayes.shares.api.domain.ShareProvider;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "share_index")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ShareIndex {
    @Id
    @GeneratedValue(generator = "uuid2")
    @ToString.Include
    @Setter
    private UUID id;

    @ToString.Include
    @EqualsAndHashCode.Include
    @Embedded
    private ShareIdentity identity;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "currency_code", nullable = false)
    @Convert(converter = CurrencyConverter.class)
    private Currency currency;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name ="provider", nullable = false)
    private ShareProvider provider;


    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Embeddable
    public static class ShareIdentity implements Serializable {
        @EqualsAndHashCode.Include
        @Column(name = "isin", nullable = false)
        private String isin;

        @EqualsAndHashCode.Include
        @Column(name = "ticker_symbol", nullable = false)
        private String tickerSymbol;

        public String toString() {
            return "[isin: " + isin + ", ticker: " + tickerSymbol + "]";
        }
    }
}
