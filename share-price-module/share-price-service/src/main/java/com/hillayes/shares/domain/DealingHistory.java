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
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class DealingHistory {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "share_holding_id", nullable = false)
    private UUID shareHoldingId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "market_date", nullable = false)
    private LocalDate marketDate;

    @Builder.Default
    @ToString.Include
    @Column(name = "is_purchase")
    private boolean purchase = true;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
}
