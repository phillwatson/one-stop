package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountTransaction;
import jakarta.enterprise.context.ApplicationScoped;

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

    public Page<AccountTransaction> findByFilter(TransactionFilter filter,
                                                 int page,
                                                 int pageSize) {
        String query = filter.toQuery();
        Map<String, Object> params = filter.toParams();

        return pageAll(query.toString(), page, pageSize,
            OrderBy.by("bookingDateTime", OrderBy.Direction.Descending), params);
    }
}
