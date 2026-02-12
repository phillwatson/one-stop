package com.hillayes.ftmarket.api.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity(name = "isin_issue_lookup")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class IsinIssueLookup {
    /**
     * The fund or equity International Securities Identification Number.
     */
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "isin", nullable = false)
    private String isin;

    /**
     * The FT internal identifier for the fund or equity.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "issue_id", nullable = false)
    private String issueId;

    /**
     * The name of the fund or equity.
     */
    @ToString.Include
    @Column(name = "name")
    private String name;

    /**
     * The currency in which the prices are delivered.
     */
    @ToString.Include
    @Column(name = "currency_code")
    private String currencyCode;

    /**
     * The monetary units in which the price information is delivered.
     */
    @ToString.Include
    @Column(name = "currency_units")
    @Enumerated(EnumType.STRING)
    private CurrencyUnits currencyUnits;
}
