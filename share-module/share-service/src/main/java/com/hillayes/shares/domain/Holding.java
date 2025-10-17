package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "holding")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Holding {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "share_index_id", nullable = false)
    private UUID shareIndexId;

    @lombok.Builder.Default
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    protected Instant dateCreated = Instant.now();

    @OneToMany(mappedBy = "holding", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private Set<DealingHistory> dealings = new HashSet<>();

    /**
     * Records a purchase of shares against this holding.
     * This ensures that the relationships are maintained correctly.
     *
     * @param date the date on which the transaction occurred.
     * @param quantity the number of shares involved.
     * @param price the price of each share.
     * @return the newly added purchase.
     */
    public DealingHistory buy(LocalDate date, int quantity, BigDecimal price) {
        return add(date, Math.abs(quantity), price);
    }

    /**
     * Records a sale of shares against this holding.
     * This ensures that the relationships are maintained correctly.
     *
     * @param date the date on which the transaction occurred.
     * @param quantity the number of shares involved.
     * @param price the price of each share.
     * @return the newly added sale.
     */
    public DealingHistory sell(LocalDate date, int quantity, BigDecimal price) {
        return add(date, -Math.abs(quantity), price);
    }

    private DealingHistory add(LocalDate date, int quantity, BigDecimal price) {
        DealingHistory sale = DealingHistory.builder()
            .holding(this)
            .marketDate(date)
            .quantity(quantity)
            .price(price)
            .build();

        dealings.add(sale);
        return sale;
    }
}
