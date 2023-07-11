package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_balance")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class AccountBalance {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "balance_type", nullable = false)
    private String balanceType;

    @Column(name = "reference_date", nullable = true)
    private LocalDate referenceDate;

    @Column(name = "last_committed_transaction", nullable = true)
    public String lastCommittedTransaction;
}
