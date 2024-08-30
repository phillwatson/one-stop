package com.hillayes.rail.domain;

import com.hillayes.commons.MonetaryAmount;
import lombok.*;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@ToString
public class TransactionMovement {
    // the start of the period covered (inclusive).
    private LocalDate fromDate;

    // the end of the period covered (exclusive)
    private LocalDate toDate;

    // the stats for the credit transactions with the period covered
    private MovementSummary credits;

    // the stats for the debit transactions with the period covered
    private MovementSummary debits;

    public record MovementSummary(
        // the number of transactions summarised
        long count,

        // the total value of the transactions summarised
        MonetaryAmount amount
    ) {}
}
