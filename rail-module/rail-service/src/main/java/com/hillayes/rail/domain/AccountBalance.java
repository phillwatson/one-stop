package com.hillayes.rail.domain;

import com.hillayes.commons.MonetaryAmount;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "rails", name = "account_balance")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountBalance {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @Column(name = "account_id", nullable = false)
    @EqualsAndHashCode.Include
    private UUID accountId;

    @lombok.Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Column(name = "reference_date", nullable = true)
    @EqualsAndHashCode.Include
    private Instant referenceDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency_code"))
    })
    private MonetaryAmount amount;

    @Column(name = "balance_type", nullable = false)
    private String balanceType;
}
