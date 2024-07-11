package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class AccountTransactionRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    CategoryRepository categoryRepository;

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
        TransactionFilter filter = TransactionFilter.builder()
            .userId(account.getUserId())
            .accountId(account.getId())
            .build();
        Page<AccountTransaction> result = fixture.findByFilter(filter, 0, 1);

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
        TransactionFilter filter = TransactionFilter.builder()
            .userId(consent.getUserId())
            .accountId(account.getId())
            .fromDate(Instant.now().minus(Duration.ofDays(21)))
            .toDate(Instant.now())
            .build();
        Page<AccountTransaction> result =
            fixture.findByFilter(filter, 0, 10 );

        // then: the results contain the transaction with the date range
        assertFalse(result.isEmpty());
        assertEquals(2, result.getContentSize());

        // and: the transactions belong to the identified user
        result.forEach(transaction -> assertEquals(consent.getUserId(), transaction.getUserId()));

        // and: the transactions are within the date range
        result.forEach(transaction -> {
            assertTrue(transaction.getBookingDateTime().compareTo(filter.getFromDate()) >= 0);
            assertTrue(transaction.getBookingDateTime().isBefore(filter.getToDate()));
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
        accounts.forEach(account -> {
            TransactionFilter filter = TransactionFilter.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .fromDate(Instant.now().minus(Duration.ofDays(21)))
                .toDate(Instant.now())
                .build();
            Page<AccountTransaction> result =
                fixture.findByFilter(filter, 0, 100);

            // then: the results contain the transaction with the date range
            assertFalse(result.isEmpty());
            assertEquals(2, result.getContentSize());

            // and: each transaction belongs to the identified account
            result.forEach(transaction -> assertEquals(account.getId(), transaction.getAccountId()));

            // and: the transactions are within the date range
            result.forEach(transaction -> {
                assertTrue(transaction.getBookingDateTime().compareTo(filter.getFromDate()) >= 0);
                assertTrue(transaction.getBookingDateTime().isBefore(filter.getToDate()));
            });
        });
    }

    @Test
    public void testFilterAdditionalInfo() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: several linked accounts
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Account account = accountRepository.save(mockAccount(consent));
            accounts.add(account);

            // and: a list of transactions
            List<AccountTransaction> transactions = new ArrayList<>();
            IntStream.range(0, 5).forEach(index ->
                transactions.add(TestData.mockAccountTransaction(transaction -> {
                    transaction.id(null);
                    transaction.accountId(account.getId());
                    transaction.userId(account.getUserId());
                    transaction.additionalInformation("Transaction " + index);
                }))
            );

            fixture.saveAll(transactions);
            fixture.flush();
        }

        // when: the transactions are returned by date range for each account
        accounts.forEach(account -> {
            TransactionFilter filter = TransactionFilter.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .info("Transaction 2")
                .build();
            Page<AccountTransaction> result =
                fixture.findByFilter(filter, 0, 100);

            // then: the results contain the transaction with the date range
            assertFalse(result.isEmpty());
            assertEquals(1, result.getContentSize());

            // and: each transaction belongs to the identified account
            result.forEach(transaction -> assertEquals(account.getId(), transaction.getAccountId()));

            // and: the transactions are within the date range
            result.forEach(transaction -> {
                assertEquals("Transaction 2", transaction.getAdditionalInformation());
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
        TransactionFilter filter = TransactionFilter.builder()
            .userId(account.getUserId())
            .accountId(account.getId())
            .build();
        Page<AccountTransaction> page = fixture.findByFilter(filter, 0, 3);

        // then: the first page transactions are returned by account ID
        assertNotNull(page);
        assertEquals(3, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(0, page.getPageIndex());
        assertEquals(3, page.getPageSize());

        // when: the second page transactions are returned by account ID
        page = fixture.findByFilter(filter, 1, 3);

        // then: the second page transactions are returned by account ID
        assertNotNull(page);
        assertEquals(3, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(1, page.getPageIndex());
        assertEquals(3, page.getPageSize());

        // when: the last page transactions are returned by account ID
        page = fixture.findByFilter(filter, 2, 3);

        // then: the last page transactions are returned by account ID
        assertNotNull(page);
        assertEquals(2, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(2, page.getPageIndex());
        assertEquals(3, page.getPageSize());

        // when: the page above the last is selected
        page = fixture.findByFilter(filter, 12, 3);

        // then: the page above the last is selected
        assertNotNull(page);
        assertEquals(0, page.getContentSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(8, page.getTotalCount());
        assertEquals(12, page.getPageIndex());
        assertEquals(3, page.getPageSize());
    }

    @Test
    public void testFindTotals() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: several linked accounts
        Map<Account, List<AccountTransaction>> accounts = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            Account account = accountRepository.save(mockAccount(consent));
            List<AccountTransaction> transactions = new ArrayList<>();
            accounts.put(account, transactions);

            // and: a list of transactions for each account
            LocalDate bookingDate = LocalDate.now().minusWeeks(5);
            AtomicInteger index = new AtomicInteger(0);
            while (bookingDate.isBefore(LocalDate.now())) {
                transactions.add(mockTransaction(account, bookingDate, transaction -> {
                    transaction.creditorName(account.getId() + "-Creditor-" + index.get());
                    transaction.additionalInformation(account.getId() + "-Info-" + index.get());
                    transaction.reference(account.getId() + "-Reference-" + index.get());
                }));

                bookingDate = bookingDate.plusWeeks(1);
                index.getAndIncrement();
            }
            fixture.saveAll(transactions);
            fixture.flush();
        }

        // when: the transactions totals are requested for each account by creditor
        accounts.forEach((account, transactions) -> {
            TransactionFilter filter = TransactionFilter.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .creditor("Creditor-2")
                .build();
            List<MonetaryAmount> totals = fixture.findTotals(filter);

            // then: the totals are returned
            assertNotNull(totals);

            // and: the totals are correct
            assertEquals(1, totals.size());

            // and: the total amount is correct
            Long expected = transactions.stream()
                .filter(transaction -> transaction.getCreditorName().contains("Creditor-2"))
                .map(t -> t.getAmount().getAmount())
                .reduce(Long::sum)
                .orElse(0L);
            assertEquals(expected, totals.get(0).getAmount());
        });

        // when: the transactions totals are requested for each account by reference
        accounts.forEach((account, transactions) -> {
            TransactionFilter filter = TransactionFilter.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .reference("Reference-2")
                .build();
            List<MonetaryAmount> totals = fixture.findTotals(filter);

            // then: the totals are returned
            assertNotNull(totals);

            // and: the totals are correct
            assertEquals(1, totals.size());

            // and: the total amount is correct
            Long expected = transactions.stream()
                .filter(transaction -> transaction.getReference().contains("Reference-2"))
                .map(t -> t.getAmount().getAmount())
                .reduce(Long::sum)
                .orElse(0L);
            assertEquals(expected, totals.get(0).getAmount());
        });

        // when: the transactions totals are requested for each account by info
        accounts.forEach((account, transactions) -> {
            TransactionFilter filter = TransactionFilter.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .info("Info-2")
                .build();
            List<MonetaryAmount> totals = fixture.findTotals(filter);

            // then: the totals are returned
            assertNotNull(totals);

            // and: the totals are correct
            assertEquals(1, totals.size());

            // and: the total amount is correct
            Long expected = transactions.stream()
                .filter(transaction -> transaction.getAdditionalInformation().contains("Info-2"))
                .map(t -> t.getAmount().getAmount())
                .reduce(Long::sum)
                .orElse(0L);
            assertEquals(expected, totals.get(0).getAmount());
        });
    }

    @Test
    public void testGetStatistics() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a collection of categories with selectors for the account's transactions
        Map<Category, List<AccountTransaction>> categoryTransactions = Map.of(
            Category.builder().userId(consent.getUserId()).name("category 1").build()
                .addSelector(account.getId(), builder -> builder.infoContains("info 1").build()), new ArrayList<>(),
            Category.builder().userId(consent.getUserId()).name("category 2").build()
                .addSelector(account.getId(), builder -> builder.refContains("ref 2").build()), new ArrayList<>(),
            Category.builder().userId(consent.getUserId()).name("category 3").build()
                .addSelector(account.getId(), builder -> builder.creditorContains("info 3").build()), new ArrayList<>(),
            Category.builder().userId(consent.getUserId()).name("category 4").build()
                .addSelector(account.getId(), builder -> builder.infoContains("creditor 4").build()), new ArrayList<>()
        );
        categoryRepository.saveAll(categoryTransactions.keySet());

        // and: a collection of transactions for the account
        // and: a match for each selector in each category
        categoryTransactions.keySet().forEach(category -> {
            category.getSelectors().stream().findFirst().ifPresent(selector ->
                categoryTransactions.get(category).add(
                    fixture.save(TestData.mockAccountTransaction(account, transaction -> {
                        transaction.id(null)
                            .bookingDateTime(now.minus(Duration.ofDays(5)))
                            .amount(MonetaryAmount.of("GBP", 100));
                        if (Strings.isNotBlank(selector.getCreditorContains())) {
                            transaction.creditorName("contains " + selector.getCreditorContains() + " text");
                        } else if (Strings.isNotBlank(selector.getRefContains())) {
                            transaction.reference("contains " + selector.getRefContains() + " text");
                        } else if (Strings.isNotBlank(selector.getInfoContains())) {
                            transaction.additionalInformation("contains " + selector.getInfoContains() + " text");
                        }
                    }))
                )
            );
        });
        fixture.flush();

        // when: the statistics are retrieved for each category
        categoryTransactions.keySet().forEach(category -> {
            List<AccountTransaction> expected = categoryTransactions.get(category);
            List<AccountTransaction> actual =
                    fixture.findByCategory(consent.getUserId(), category.getId(), now.minus(Duration.ofDays(7)), now);

            // then: the expected transactions are returned
            assertNotNull(actual, "Category: " + category.getName());
            assertEquals(expected.size(), actual.size(), "Category: " + category.getName());
            assertTrue(expected.containsAll(actual), "Category: " + category.getName());
        });
    }

    private UserConsent mockUserConsent() {
        return TestData.mockUserConsent(UUID.randomUUID(), consent -> {
            consent.id(null);
        });
    }

    private Account mockAccount(UserConsent consent) {
        return TestData.mockAccount(consent.getUserId(), account -> {
            account.id(null);
            account.userConsentId(consent.getId());
            account.institutionId(consent.getInstitutionId());
        });
    }

    private AccountTransaction mockTransaction(Account account, LocalDate bookingDate) {
        return mockTransaction(account, bookingDate, null);
    }

    private AccountTransaction mockTransaction(Account account, LocalDate bookingDate,
                                               Consumer<AccountTransaction.Builder> modifier) {
        return TestData.mockAccountTransaction(transaction -> {
            transaction.id(null);
            transaction.userId(account.getUserId());
            transaction.accountId(account.getId());
            transaction.bookingDateTime(bookingDate.atStartOfDay(ZoneOffset.UTC).toInstant());
            if (modifier != null) {
                modifier.accept(transaction);
            }
        });
    }
}
