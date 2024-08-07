package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AccountTransactionRepository extends RepositoryBase<AccountTransaction, UUID> {
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

    public List<AccountTransaction> findByCategory(UUID userId, UUID categoryId,
                                                   Instant startDate, Instant endDate) {
        return getEntityManager().createNativeQuery(SELECT_BY_CATEGORY, AccountTransaction.class)
            .setParameter("userId", userId)
            .setParameter("categoryId", categoryId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    public List<AccountTransaction> findUncategorised(UUID userId, UUID groupId,
                                                      Instant startDate, Instant endDate) {
        return getEntityManager().createNativeQuery(SELECT_BY_NON_CATEGORY, AccountTransaction.class)
            .setParameter("userId", userId)
            .setParameter("groupId", groupId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }
}
