package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dealing_history")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class DealingHistory {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @Getter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Include
    @Column(name = "holding_id", insertable=false, updatable=false)
    private UUID holdingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id")
    private Holding holding;

    @Column(name = "date_executed", nullable = false)
    @EqualsAndHashCode.Include
    private LocalDate dateExecuted;

    /**
     * The number of shares bought or sold. Sold shares are recorded as a negative number.
     */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /**
     * The price paid or received for each share.
     */
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    /**
     * Returns the value of this dealing; its price multiplied by its quantity.
     * If the dealing is a sale, the result will be negative.
     */
    @Transient
    public BigDecimal getValue() {
        return BigDecimal.valueOf(quantity * price.doubleValue());
    }
}
