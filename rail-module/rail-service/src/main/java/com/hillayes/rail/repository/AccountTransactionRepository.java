package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.CategoryGroup;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class AccountTransactionRepository extends RepositoryBase<AccountTransaction, UUID> {
    private static final String SELECT_BY_USER =
        "SELECT t FROM AccountTransaction t " +
            "WHERE t.userId = :userId " +
            "AND t.bookingDateTime >= :startDate " +
            "AND t.bookingDateTime < :endDate " +
            "ORDER BY t.bookingDateTime DESC";

    private static final String SELECT_BY_ACCOUNT =
        "SELECT t FROM AccountTransaction t " +
            "WHERE t.userId = :userId " +
            "AND t.accountId = :accountId " +
            "AND t.bookingDateTime >= :startDate " +
            "AND t.bookingDateTime < :endDate " +
            "ORDER BY t.bookingDateTime DESC";

    private static final String SELECT_BY_CATEGORY =
        "select t.* from rails.account_transaction t " +
        "where t.user_id = :userId " +
        "and t.booking_datetime >= :startDate " +
        "and t.booking_datetime < :endDate " +
        "and exists ( " +
        "  select 1 from rails.category_selector cs where " +
        "    cs.category_id = :categoryId " +
        "    and cs.account_id = t.account_id " +
        "    and (cs.info_contains is null or t.additional_information like concat('%', cs.info_contains, '%')) " +
        "    and (cs.ref_contains is null or t.reference like concat('%', cs.ref_contains, '%')) " +
        "    and (cs.creditor_contains is null or t.creditor_name like concat('%', cs.creditor_contains, '%')) " +
        ") " +
        "order by t.booking_datetime desc";

    private static final String SELECT_BY_NON_CATEGORY =
        "select * from rails.account_transaction t " +
        "where t.user_id = :userId " +
        "and t.booking_datetime >= :startDate " +
        "and t.booking_datetime < :endDate " +
        "and not exists ( " +
        "  select 1 from rails.category_selector cs " +
        "  inner join rails.category c on c.group_id = :groupId and c.id = cs.category_id " +
        "  where cs.account_id = t.account_id " +
        "    and (cs.info_contains is null or t.additional_information like concat('%', cs.info_contains, '%')) " +
        "    and (cs.ref_contains is null or t.reference like concat('%', cs.ref_contains, '%')) " +
        "    and (cs.creditor_contains is null or t.creditor_name like concat('%', cs.creditor_contains, '%')) " +
        ") " +
        "order by t.booking_datetime desc";

    private static final String SELECT_USERS_MOVEMENTS =
        "select " +
            "  CAST(DATE_TRUNC('day', t.booking_datetime) as DATE) as from_date, " +
            "  CAST(DATE_TRUNC('day', t.booking_datetime + interval '1 day') as DATE) as to_date, " +
            "  t.currency_code, " +
            "  sum(case when t.amount > 0 then 1 else 0 end) as credit_count, " +
            "  sum(case when t.amount > 0 then t.amount else 0 end) as credit_value, " +
            "  sum(case when t.amount < 0 then 1 else 0 end) as debit_count, " +
            "  sum(case when t.amount < 0 then t.amount else 0 end) as debit_value " +
            "from rails.account_transaction t " +
            "where t.user_id = :userId " +
            "and t.booking_datetime >= :startDate " +
            "and t.booking_datetime < :endDate " +
            "group by 1, 2, 3 " +
            "order by 1 asc";

    private static final String SELECT_ACCOUNTS_MOVEMENTS =
        "select " +
        "  CAST(DATE_TRUNC('day', t.booking_datetime) as DATE) as from_date, " +
        "  CAST(DATE_TRUNC('day', t.booking_datetime + interval '1 day') as DATE) as to_date, " +
        "  t.currency_code, " +
        "  sum(case when t.amount > 0 then 1 else 0 end) as credit_count, " +
        "  sum(case when t.amount > 0 then t.amount else 0 end) as credit_value, " +
        "  sum(case when t.amount < 0 then 1 else 0 end) as debit_count, " +
        "  sum(case when t.amount < 0 then t.amount else 0 end) as debit_value " +
        "from rails.account_transaction t " +
        "where t.user_id = :userId " +
        "and t.account_id = :accountId " +
        "and t.booking_datetime >= :startDate " +
        "and t.booking_datetime < :endDate " +
        "group by 1, 2, 3 " +
        "order by 1 asc";

    /**
     * Locates the transactions whose internal ID is in the given list. The internal
     * transaction ID is assigned by the rail service.
     *
     * @param internalTransactionIds the list of internal transaction IDs.
     * @return those transactions identified in the given list.
     */
    public List<AccountTransaction> findByInternalId(List<String> internalTransactionIds) {
        return internalTransactionIds.isEmpty()
            ? List.of()
            : listAll("internalTransactionId in ?1", internalTransactionIds);
    }

    public Page<AccountTransaction> findByFilter(TransactionFilter filter,
                                                 int page,
                                                 int pageSize) {
        String query = filter.toQuery();
        Map<String, Object> params = filter.toParams();

        return pageAll(query, page, pageSize,
            OrderBy.by("bookingDateTime", OrderBy.Direction.Descending), params);
    }

    public List<MonetaryAmount> findTotals(TransactionFilter filter) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MonetaryAmount> query = builder.createQuery(MonetaryAmount.class);
        Root<AccountTransaction> root = query.from(AccountTransaction.class);

        Path<Object> amount = root.get("amount");
        query.multiselect(
                amount.get("currency"),
                builder.sum(amount.get("amount")))
            .groupBy(amount.get("currency"))
            .where(filter.toPredicate(builder, root));
        return entityManager.createQuery(query).getResultList();
    }

    public List<AccountTransaction> listAll(Collection<UUID> transactionIds){
        return listAll("id in ?1", transactionIds);
    }

    public List<AccountTransaction> findByUser(UUID userId,
                                               Instant startDateInclusive,
                                               Instant endDateExclusive) {
        return getEntityManager().createQuery(SELECT_BY_USER)
            .setParameter("userId", userId)
            .setParameter("startDate", startDateInclusive)
            .setParameter("endDate", endDateExclusive)
            .getResultList();
    }

    public List<AccountTransaction> findByAccount(UUID userId, UUID accountId,
                                                  Instant startDateInclusive,
                                                  Instant endDateExclusive) {
        return getEntityManager().createQuery(SELECT_BY_ACCOUNT)
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .setParameter("startDate", startDateInclusive)
            .setParameter("endDate", endDateExclusive)
            .getResultList();
    }

    public List<MovementProjection> getMovementStats(UUID userId, UUID accountId,
                                                     Instant startDateInclusive, Instant endDateExclusive) {
        String query = (accountId == null) ? SELECT_USERS_MOVEMENTS : SELECT_ACCOUNTS_MOVEMENTS;
        return getEntityManager().createNativeQuery(query, MovementProjection.class)
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .setParameter("startDate", startDateInclusive)
            .setParameter("endDate", endDateExclusive)
            .getResultList();
    }

    public List<AccountTransaction> findByCategoryGroup(CategoryGroup categoryGroup,
                                                        Instant startDateInclusive,
                                                        Instant endDateExclusive,
                                                        boolean includeUncategorised) {
        // collate all transactions for each category in the group
        List<AccountTransaction> result = categoryGroup.getCategories().stream()
            .parallel()
            .map(category -> findByCategory(categoryGroup.getUserId(), category.getId(), startDateInclusive, endDateExclusive))
            .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);

        if (includeUncategorised) {
            // add the uncategorised transactions
            result.addAll(
                findUncategorised(categoryGroup.getUserId(), categoryGroup.getId(), startDateInclusive, endDateExclusive)
            );
        }

        // sort results by booking date - descending
        result.sort(Comparator.comparing(AccountTransaction::getBookingDateTime).reversed());
        return result;
    }

    public List<AccountTransaction> findByCategory(UUID userId, UUID categoryId,
                                                   Instant startDateInclusive,
                                                   Instant endDateExclusive) {
        return getEntityManager().createNativeQuery(SELECT_BY_CATEGORY, AccountTransaction.class)
            .setParameter("userId", userId)
            .setParameter("categoryId", categoryId)
            .setParameter("startDate", startDateInclusive)
            .setParameter("endDate", endDateExclusive)
            .getResultList();
    }

    public List<AccountTransaction> findUncategorised(UUID userId, UUID groupId,
                                                      Instant startDateInclusive,
                                                      Instant endDateExclusive) {
        return getEntityManager().createNativeQuery(SELECT_BY_NON_CATEGORY, AccountTransaction.class)
            .setParameter("userId", userId)
            .setParameter("groupId", groupId)
            .setParameter("startDate", startDateInclusive)
            .setParameter("endDate", endDateExclusive)
            .getResultList();
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @RegisterForReflection
    public static class MovementProjection {
        @Column(name = "from_date", nullable = false)
        public java.sql.Date fromDate;

        @Column(name = "to_date", nullable = false)
        public java.sql.Date toDate;

        @Column(name = "currency_code", nullable = false)
        public String currencyCode;

        @Column(name = "credit_count", nullable = false)
        public long creditCount;

        @Column(name = "credit_value", nullable = false)
        public BigDecimal creditValue;

        @Column(name = "debit_count", nullable = false)
        public long debitCount;

        @Column(name = "debit_value", nullable = false)
        public BigDecimal debitValue;
    }
}
