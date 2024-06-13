package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AccountTransactionRepository extends RepositoryBase<AccountTransaction, UUID> {
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

    public Page<AccountTransaction> findByFilter(UUID userId,
                                                 TransactionFilter filter,
                                                 int page,
                                                 int pageSize) {
        StringBuilder query = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap();
        params.put("userId", userId);

        if (filter.getAccountId() != null) {
            query.append(" AND accountId = :accountId");
            params.put("accountId", filter.getAccountId());
        }
        if (filter.getFromDate() != null) {
            query.append(" AND bookingDateTime >= :fromDate");
            params.put("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            query.append(" AND bookingDateTime < :toDate");
            params.put("toDate", filter.getToDate());
        }
        if (filter.getMinAmount() != null) {
            query.append(" AND amount >= :minAmount");
            params.put("minAmount", MonetaryAmount.of("GBP", filter.getMinAmount()));
        }
        if (filter.getMaxAmount() != null) {
            query.append(" AND amount <= :maxAmount");
            params.put("maxAmount", MonetaryAmount.of("GBP", filter.getMaxAmount()));
        }
        if (filter.getReference() != null) {
            query.append(" AND reference like :reference");
            params.put("reference", "%" + filter.getReference() + "%");
        }
        if (filter.getInfo() != null) {
            query.append(" AND additionalInformation like :info");
            params.put("info", "%" + filter.getInfo() + "%");
        }
        if (filter.getCreditor() != null) {
            query.append(" AND creditorName like :creditor");
            params.put("creditor", "%" + filter.getCreditor() + "%");
        }

        return pageAll(query.toString(), page, pageSize,
            OrderBy.by("bookingDateTime", OrderBy.Direction.Descending), params);
    }
}
