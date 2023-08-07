package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AccountTransaction;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AccountTransactionRepository extends RepositoryBase<AccountTransaction, UUID> {
    public Page<AccountTransaction> findByUserId(UUID userId, Sort sortBy, int pageNumber, int pageSize) {
        return findByPage(find("userId", sortBy, userId),
            pageNumber, pageSize);
    }

    public Page<AccountTransaction> findByAccountId(UUID accountId, Sort sortBy, int pageNumber, int pageSize) {
        return findByPage(find("accountId", sortBy, accountId), pageNumber, pageSize);
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
        return find("userId = :userId AND bookingDateTime >= :fromDate AND bookingDateTime < :toDate",
            Sort.by("bookingDateTime"),
            Parameters
                .with("userId", userId)
                .and("fromDate", fromDate)
                .and("toDate", toDate))
            .list();
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
        return find("accountId = :accountId AND bookingDateTime >= :fromDate AND bookingDateTime < :toDate",
            Sort.by("bookingDateTime"),
            Parameters
                .with("accountId", accountId)
                .and("fromDate", fromDate)
                .and("toDate", toDate))
            .list();
    }
}
