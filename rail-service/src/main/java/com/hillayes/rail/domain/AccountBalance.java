package com.hillayes.rail.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
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
    public double amount;

    @Column(name = "currency", nullable = false)
    public String currency;

    @Column(name = "balance_type", nullable = false)
    public String balanceType;

    @Column(name = "reference_date", nullable = true)
    public LocalDate referenceDate;
}
