package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_balance")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountBalance {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    @EqualsAndHashCode.Include
    private UUID accountId;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Column(name = "reference_date", nullable = true)
    @EqualsAndHashCode.Include
    private LocalDate referenceDate;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "balance_type", nullable = false)
    private String balanceType;
}
