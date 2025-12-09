package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.domain.AccountTransaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
    private Double minAmount;
    // the maximum amount for the transaction.
    private Double maxAmount;
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
     * Tests if the filter is empty, i.e. the filter will not result in a sub-set of
     * transactions.
     */
    public boolean isEmpty() {
        return (getAccountId() != null) // when null, this includes all accounts - therefore, a non-empty filter
            && (getFromDate() == null)
            && (getToDate() == null)
            && (getMinAmount() == null)
            && (getMaxAmount() == null)
            && (getReference() == null)
            && (getInfo() == null)
            && (getCreditor() == null);
    }

    /**
     * Returns a map of the filter parameters, keyed on the names used in the WHERE clause
     * selection.
     * Only those parameters with a non-null value are included.
     */
    public Map<String, Object> toParams() {
        Map<String, Object> params = new HashMap<>();

        if (getUserId() != null) {
            params.put("userId", getUserId());
        }
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
        List<String> query = new ArrayList<>();

        if (getUserId() != null) {
            query.add("userId = :userId");
        }
        if (getAccountId() != null) {
            query.add("accountId = :accountId");
        }
        if (getFromDate() != null) {
            query.add("bookingDateTime >= :fromDate");
        }
        if (getToDate() != null) {
            query.add("bookingDateTime < :toDate");
        }
        if (getMinAmount() != null) {
            query.add("amount >= :minAmount");
        }
        if (getMaxAmount() != null) {
            query.add("amount <= :maxAmount");
        }
        if (getReference() != null) {
            query.add("reference like :reference");
        }
        if (getInfo() != null) {
            query.add("additionalInformation like :info");
        }
        if (getCreditor() != null) {
            query.add("creditorName like :creditor");
        }

        return String.join(" AND ", query);
    }

    public Predicate toPredicate(CriteriaBuilder builder, Root<AccountTransaction> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (getUserId() != null) {
            predicates.add(builder.equal(root.get("userId"), getUserId()));
        }
        if (getAccountId() != null) {
            predicates.add(builder.equal(root.get("accountId"), getAccountId()));
        }
        if (getFromDate() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("bookingDateTime"), getFromDate()));
        }
        if (getToDate() != null) {
            predicates.add(builder.lessThan(root.get("bookingDateTime"), getToDate()));
        }
        if (getMinAmount() != null) {
            MonetaryAmount amount = MonetaryAmount.of("GBP", getMinAmount());
            predicates.add(builder.ge(root.get("amount").get("amount"), amount.getAmount()));
        }
        if (getMaxAmount() != null) {
            MonetaryAmount amount = MonetaryAmount.of("GBP", getMaxAmount());
            predicates.add(builder.le(root.get("amount").get("amount"), amount.getAmount()));
        }
        if (getReference() != null) {
            predicates.add(builder.like(root.get("reference"), "%" + getReference() + "%"));
        }
        if (getInfo() != null) {
            predicates.add(builder.like(root.get("additionalInformation"), "%" + getInfo() + "%"));
        }
        if (getCreditor() != null) {
            predicates.add(builder.like(root.get("creditorName"), "%" + getCreditor() + "%"));
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }
}
