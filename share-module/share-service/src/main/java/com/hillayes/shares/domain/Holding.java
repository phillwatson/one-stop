package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
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

    /**
     * The ID of the containing portfolio.
     */
    @EqualsAndHashCode.Include
    @Column(name = "portfolio_id", updatable=false)
    private UUID portfolioId;

    /**
     * The ID of the referenced share index. Only used for hash-code and equals.
     */
    @Getter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Include
    @Column(name = "share_index_id", insertable=false, updatable=false)
    private UUID shareIndexId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_index_id")
    private ShareIndex shareIndex;

    @lombok.Builder.Default
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    protected Instant dateCreated = Instant.now();

    @OneToMany(mappedBy = "holding", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("dateExecuted ASC")
    @lombok.Builder.Default
    private List<DealingHistory> dealings = new ArrayList<>();

    /**
     * Records a purchase of shares against this holding.
     * This ensures that the relationships are maintained correctly.
     *
     * @param dateExecuted the date on which the transaction occurred.
     * @param quantity the number of shares involved.
     * @param price the price of each share.
     * @return the newly added purchase.
     */
    public DealingHistory buy(LocalDate dateExecuted, int quantity, BigDecimal price) {
        return add(dateExecuted, Math.abs(quantity), price);
    }

    /**
     * Records a sale of shares against this holding.
     * This ensures that the relationships are maintained correctly.
     *
     * @param dateExecuted the date on which the transaction occurred.
     * @param quantity the number of shares involved.
     * @param price the price of each share.
     * @return the newly added sale.
     */
    public DealingHistory sell(LocalDate dateExecuted, int quantity, BigDecimal price) {
        return add(dateExecuted, -Math.abs(quantity), price);
    }

    private DealingHistory add(LocalDate dateExecuted, int quantity, BigDecimal price) {
        DealingHistory dealing = DealingHistory.builder()
            .holding(this)
            .dateExecuted(dateExecuted)
            .quantity(quantity)
            .price(price)
            .build();

        dealings.add(dealing);
        return dealing;
    }

    @Transient
    public Currency getCurrency() {
        return getShareIndex().getCurrency();
    }

    @Transient
    public int getQuantity() {
        return getDealings().stream().mapToInt(DealingHistory::getQuantity).sum();
    }

    @Transient
    public BigDecimal getTotalCost() {
        return BigDecimal.valueOf(
            getDealings().stream()
                .mapToDouble(d -> d.getPrice().doubleValue() * d.getQuantity())
                .sum()
        );
    }
}
