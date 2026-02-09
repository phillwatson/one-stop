package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "share_trade")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ShareTrade {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false, updatable=false)
    private UUID userId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "portfolio_id", nullable = false, updatable=false)
    private UUID portfolioId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "share_index_id", nullable = false)
    private UUID shareIndexId;

    @Column(name = "date_executed", nullable = false)
    @Setter
    @EqualsAndHashCode.Include
    private LocalDate dateExecuted;

    /**
     * The number of shares bought or sold. Sold shares are recorded as a negative number.
     */
    @Column(name = "quantity", nullable = false)
    @Setter
    private BigDecimal quantity;

    /**
     * The price paid or received for each share.
     */
    @Column(name = "price", nullable = false)
    @Setter
    private BigDecimal price;

    /**
     * Returns the value of this dealing; its price multiplied by its quantity.
     * If the dealing is a sale, the result will be negative.
     */
    @Transient
    public BigDecimal getValue() {
        return quantity.multiply(price);
    }
}
