package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

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
    // a NULl value instance of the transaction 
    public static final TransactionFilter NULL = TransactionFilter.builder().build();

    // the user to which the transaction must belong.
    private UUID userId;
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
     * Sets the date range for the transaction  The date range is inclusive of the
     * fromDate and exclusive of the toDate.
     *
     * @param from the date from which the transaction should be included - inclusive.
     * @param to the date to which the transaction should be included - exclusive.
     * @return the transaction 
     */
    public TransactionFilter dateRange(LocalDate from, LocalDate to) {
        // convert dates to instant
        fromDate = (from == null) ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        toDate = (to == null) ? null : to.atStartOfDay(ZoneOffset.UTC).toInstant();
        return this;
    }

    /**
     * Returns a map of the filter parameters, keyed on the names used in the WHERE clause
     * selection.
     * Only those parameters with a non-null value are included.
     */
    public Map<String, Object> toParams() {
        Map<String, Object> params = new HashMap();

        if (getAccountId() != null) {
            params.put("accountId", getAccountId());
        }
        if (getFromDate() != null) {
            params.put("fromDate", getFromDate());
        }
        if (getToDate() != null) {
            params.put("toDate", getToDate());
        }
        if (getMinAmount() != null) {
            params.put("minAmount", MonetaryAmount.of("GBP", getMinAmount()));
        }
        if (getMaxAmount() != null) {
            params.put("maxAmount", MonetaryAmount.of("GBP", getMaxAmount()));
        }
        if (getReference() != null) {
            params.put("reference", "%" + getReference() + "%");
        }
        if (getInfo() != null) {
            params.put("info", "%" + getInfo() + "%");
        }
        if (getCreditor() != null) {
            params.put("creditor", "%" + getCreditor() + "%");
        }

        return params;
    }

    /**
     * Constructs a SQL WHERE clause selection based on the non-null values of this filter.
     * To avoid SQL injection, the parameter values themselves are not copied to the string
     * but, instead, are represented by parameter placeholders. The names of the parameter
     * placeholders match those used in the parameter map (see {@link #toParams()}).
     */
    public String toQuery() {
        List<String> projection = new ArrayList<>();

        if (getAccountId() != null) {
            projection.add("userId = :userId");
        }
        if (getAccountId() != null) {
            projection.add("accountId = :accountId");
        }
        if (getFromDate() != null) {
            projection.add("bookingDateTime >= :fromDate");
        }
        if (getToDate() != null) {
            projection.add("bookingDateTime < :toDate");
        }
        if (getMinAmount() != null) {
            projection.add("amount >= :minAmount");
        }
        if (getMaxAmount() != null) {
            projection.add("amount <= :maxAmount");
        }
        if (getReference() != null) {
            projection.add("reference like :reference");
        }
        if (getInfo() != null) {
            projection.add("additionalInformation like :info");
        }
        if (getCreditor() != null) {
            projection.add("creditorName like :creditor");
        }

        return String.join("AND", projection);
    }
}
