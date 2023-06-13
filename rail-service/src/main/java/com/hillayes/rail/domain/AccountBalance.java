package com.hillayes.rail.domain;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity(name = "account_balance")
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
