package com.hillayes.rail.api.domain;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Transaction {
    private String id;
    private String originalTransactionId;
    private Instant dateBooked;
    private Instant dateValued;
    private MonetaryAmount amount;
    private String description;
    private String reference;
    private String creditor;
}
