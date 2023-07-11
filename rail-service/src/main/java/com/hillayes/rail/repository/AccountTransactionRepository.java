package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    public Page<AccountTransaction> findByUserId(UUID userId, Pageable pageable);

    public Page<AccountTransaction> findByAccountId(UUID accountId, Pageable pageable);

    /**
     * Returns the transactions, for the identified user, whose booking-datetime is between
     * the given date range; and ordered by the booking-datetime ascending.
     *
     * @param userId the user whose transactions are to be returned.
     * @param fromDate the date-time from which the search will begin (inclusive)
     * @param toDate the date-time to which the search will end (exclusive).
     * @return the ordered list of transaction between the given dates.
     */
    @Query("select t from AccountTransaction t " +
        "where t.userId = :userId " +
        "and t.bookingDateTime >= :fromDate " +
        "and t.bookingDateTime < :toDate " +
        "order by t.bookingDateTime")
    public List<AccountTransaction> findByUserAndBookingDateTimeRange(@Param("userId") UUID userId,
                                                                      @Param("fromDate") Instant fromDate,
                                                                      @Param("toDate") Instant toDate);

    /**
     * Returns the transactions, for the identified account, whose booking-datetime is between
     * the given date range; and ordered by the booking-datetime ascending.
     *
     * @param accountId the account whose transactions are to be returned.
     * @param fromDate the date-time from which the search will begin (inclusive)
     * @param toDate the date-time to which the search will end (exclusive).
     * @return the ordered list of transaction between the given dates.
     */
    @Query("select t from AccountTransaction t " +
        "where t.accountId = :accountId " +
        "and t.bookingDateTime >= :fromDate " +
        "and t.bookingDateTime < :toDate " +
        "order by t.bookingDateTime")
    public List<AccountTransaction> findByAccountAndBookingDateTimeRange(@Param("accountId") UUID accountId,
                                                                         @Param("fromDate") Instant fromDate,
                                                                         @Param("toDate") Instant toDate);
}
