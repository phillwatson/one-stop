package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AccountTransactionRepository extends RepositoryBase<AccountTransaction, UUID> {
    public Page<AccountTransaction> findByUserId(UUID userId, OrderBy sortBy, int pageNumber, int pageSize) {
        return pageAll("userId", sortBy, pageNumber, pageSize, userId);
    }

    public Page<AccountTransaction> findByAccountId(UUID accountId, OrderBy sortBy, int pageNumber, int pageSize) {
        return pageAll("accountId", sortBy, pageNumber, pageSize, accountId);
    }

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

    /**
     * Returns the transactions, for the identified user, whose booking-datetime is between
     * the given date range; and ordered by the booking-datetime ascending.
     *
     * @param userId the user whose transactions are to be returned.
     * @param fromDate the date-time from which the search will begin (inclusive)
     * @param toDate the date-time to which the search will end (exclusive).
     * @return the ordered list of transaction between the given dates.
     */
    public List<AccountTransaction> findByUserAndDateRange(UUID userId,
                                                           Instant fromDate,
                                                           Instant toDate) {
        return listAll("userId = :userId AND bookingDateTime >= :fromDate AND bookingDateTime < :toDate",
            OrderBy.by("bookingDateTime"),
            Map.of(
                "userId", userId,
                "fromDate", fromDate,
                "toDate", toDate
            ));
    }

    /**
     * Returns the transactions, for the identified account, whose booking-datetime is between
     * the given date range; and ordered by the booking-datetime ascending.
     *
     * @param accountId the account whose transactions are to be returned.
     * @param fromDate the date-time from which the search will begin (inclusive)
     * @param toDate the date-time to which the search will end (exclusive).
     * @return the ordered list of transaction between the given dates.
     */
    public List<AccountTransaction> findByAccountAndDateRange(UUID accountId,
                                                              Instant fromDate,
                                                              Instant toDate) {
        return listAll("accountId = :accountId AND bookingDateTime >= :fromDate AND bookingDateTime < :toDate",
            OrderBy.by("bookingDateTime"),
            Map.of("accountId", accountId,
                "fromDate", fromDate,
                "toDate", toDate)
        );
    }
}
