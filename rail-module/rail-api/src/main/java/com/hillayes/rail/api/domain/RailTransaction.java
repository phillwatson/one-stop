package com.hillayes.rail.api.domain;

import com.hillayes.commons.MonetaryAmount;
import lombok.*;

import java.time.Instant;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class RailTransaction {
    @EqualsAndHashCode.Include
    private String id;
    private String originalTransactionId;
    @ToString.Include
    private Instant dateBooked;
    private Instant dateValued;
    @ToString.Include
    private MonetaryAmount amount;
    private String description;
    private String reference;
    private String creditor;
}
