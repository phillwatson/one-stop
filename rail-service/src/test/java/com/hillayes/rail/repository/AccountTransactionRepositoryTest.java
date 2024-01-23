package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class AccountTransactionRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountTransactionRepository fixture;

    @Test
    public void testGetMostRecent() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a list of transactions
        List<AccountTransaction> transactions = new ArrayList<>();
        LocalDate bookingDate = LocalDate.now().minusWeeks(5);
        while (bookingDate.isBefore(LocalDate.now())) {
            transactions.add(mockTransaction(account, bookingDate));
            bookingDate = bookingDate.plusWeeks(1);
        }
        fixture.saveAll(transactions);
        fixture.flush();

        // when: the most recent transaction by bookingDate is queried
        OrderBy sort = OrderBy.by("bookingDateTime").descending();
        Page<AccountTransaction> result = fixture.findByAccountId(account.getId(), sort, 0, 1);

        // then: the result contains only the most recent transaction
        assertEquals(1, result.getContentSize());
        assertEquals(transactions.get(transactions.size() - 1), result.getContent().get(0));
    }

    @Test
    public void testByUserAndBookingDateTimeRange() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a list of transactions
        List<AccountTransaction> transactions = new ArrayList<>();
        LocalDate bookingDate = LocalDate.now().minusWeeks(5);
        while (bookingDate.isBefore(LocalDate.now())) {
            transactions.add(mockTransaction(account, bookingDate));
            bookingDate = bookingDate.plusWeeks(1);
        }
        fixture.saveAll(transactions);
        fixture.flush();

        // when: the transactions are returned by date range
        Instant dateFrom = Instant.now().minus(Duration.ofDays(21));
        Instant dateTo = Instant.now();
        List<AccountTransaction> result =
            fixture.findByUserAndDateRange(consent.getUserId(), dateFrom, dateTo);

        // then: the results contain the transaction with the date range
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());

        // and: the transactions belong to the identified user
        result.forEach(transaction -> assertEquals(consent.getUserId(), transaction.getUserId()));

        // and: the transactions are within the date range
        result.forEach(transaction -> {
            assertTrue(transaction.getBookingDateTime().compareTo(dateFrom) >= 0);
            assertTrue(transaction.getBookingDateTime().isBefore(dateTo));
        });
    }

    @Test
    public void testByAccountAndBookingDateTimeRange() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: several linked accounts
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Account account = accountRepository.save(mockAccount(consent));
            accounts.add(account);

            // and: a list of transactions
            List<AccountTransaction> transactions = new ArrayList<>();
            LocalDate bookingDate = LocalDate.now().minusWeeks(5);
            while (bookingDate.isBefore(LocalDate.now())) {
                transactions.add(mockTransaction(account, bookingDate));
                bookingDate = bookingDate.plusWeeks(1);
            }
            fixture.saveAll(transactions);
            fixture.flush();
        }

        // when: the transactions are returned by date range for each account
        Instant dateFrom = Instant.now().minus(Duration.ofDays(21));
        Instant dateTo = Instant.now();
        accounts.forEach(account -> {
            List<AccountTransaction> result =
                fixture.findByAccountAndDateRange(account.getId(), dateFrom, dateTo);

            // then: the results contain the transaction with the date range
            assertFalse(result.isEmpty());
            assertEquals(2, result.size());

            // and: each transaction belongs to the identified account
            result.forEach(transaction -> assertEquals(account.getId(), transaction.getAccountId()));

            // and: the transactions are within the date range
            result.forEach(transaction -> {
                assertTrue(transaction.getBookingDateTime().compareTo(dateFrom) >= 0);
                assertTrue(transaction.getBookingDateTime().isBefore(dateTo));
            });
        });
    }

    @Test
    public void testPagination() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a list of transactions
        List<AccountTransaction> transactions = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate bookingDate = now.minusDays(8);
        while (bookingDate.isBefore(now)) {
            transactions.add(mockTransaction(account, bookingDate));
            bookingDate = bookingDate.plusDays(1);
        }
        fixture.saveAll(transactions);
        fixture.flush();

        // when: the first page transactions are returned by account ID
        Page<AccountTransaction> page = fixture.findByAccountId(account.getId(), OrderBy.by("bookingDateTime"), 0, 3);

        // then: the first page transactions are returned by account ID
        assertNotNull(page);
        assertEquals(3, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(0, page.getPageIndex());
        assertEquals(3, page.getPageSize());

        // when: the second page transactions are returned by account ID
        page = fixture.findByAccountId(account.getId(), OrderBy.by("bookingDateTime"), 1, 3);

        // then: the second page transactions are returned by account ID
        assertNotNull(page);
        assertEquals(3, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(1, page.getPageIndex());
        assertEquals(3, page.getPageSize());

        // when: the last page transactions are returned by account ID
        page = fixture.findByAccountId(account.getId(), OrderBy.by("bookingDateTime"), 2, 3);

        // then: the last page transactions are returned by account ID
        assertNotNull(page);
        assertEquals(2, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(2, page.getPageIndex());
        assertEquals(3, page.getPageSize());

        // when: the page above the last is selected
        page = fixture.findByAccountId(account.getId(), OrderBy.by("bookingDateTime"), 12, 3);

        // then: the page above the last is selected
        assertNotNull(page);
        assertEquals(0, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(12, page.getPageIndex());
        assertEquals(3, page.getPageSize());
    }

    private UserConsent mockUserConsent() {
        return UserConsent.builder()
            .userId(UUID.randomUUID())
            .institutionId(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .agreementExpires(Instant.now().plus(Duration.ofDays(20)))
            .maxHistory(80)
            .status(ConsentStatus.GIVEN)
            .build();
    }

    private Account mockAccount(UserConsent consent) {
        return Account.builder()
            .userConsentId(consent.getId())
            .userId(consent.getUserId())
            .institutionId(consent.getInstitutionId())
            .railAccountId(UUID.randomUUID().toString())
            .build();
    }

    private AccountTransaction mockTransaction(Account account, LocalDate bookingDate) {
        return AccountTransaction.builder()
            .userId(account.getUserId())
            .accountId(account.getId())
            .internalTransactionId(UUID.randomUUID().toString())
            .bookingDateTime(bookingDate.atStartOfDay(ZoneOffset.UTC).toInstant())
            .transactionAmount(123.33)
            .transactionCurrencyCode("GBP")
            .build();
    }
}
