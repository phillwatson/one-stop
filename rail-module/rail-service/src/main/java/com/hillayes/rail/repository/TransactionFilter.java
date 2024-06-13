package com.hillayes.rail.repository;

import com.hillayes.rail.service.AccountTransactionService;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * A filter for transactions. Each property is an optional value that can be used
 * to filter the transactions.
 */
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class TransactionFilter {
    // a NULl value instance of the transaction filter.
    public static final TransactionFilter NULL = TransactionFilter.builder().build();

    // the account to which the transaction must belong.
    private UUID accountId;
    // the inclusive start of the date range for the transaction.
    private Instant fromDate;
    // the exclusive end of the date range for the transaction.
    private Instant toDate;
    // the minimum amount for the transaction.
    private Float minAmount;
    // the maximum amount for the transaction.
    private Float maxAmount;
    // the value that the transaction's reference must contain - case-insensitive.
    private String reference;
    // the value that the transaction's info must contain - case-insensitive.
    private String info;
    // the value that the transaction's creditor must contain - case-insensitive.
    private String creditor;

    /**
     * Sets the date range for the transaction filter. The date range is inclusive of the
     * fromDate and exclusive of the toDate.
     *
     * @param from the date from which the transaction should be included - inclusive.
     * @param to the date to which the transaction should be included - exclusive.
     * @return the transaction filter.
     */
    public TransactionFilter dateRange(LocalDate from, LocalDate to) {
        // convert dates to instant
        fromDate = (from == null) ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        toDate = (to == null) ? null : to.atStartOfDay(ZoneOffset.UTC).toInstant();
        return this;
    }
}
