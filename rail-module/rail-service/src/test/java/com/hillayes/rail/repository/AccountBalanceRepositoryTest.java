package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class AccountBalanceRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountBalanceRepository fixture;

    @Test
    public void testSaveAccountBalance() {
        // given: a user-consent
        UserConsent consent = createUserConsent();

        // and: linked account
        Account account = createAccounts(consent, 1).getFirst();

        // when: an account balance is saved
        AccountBalance accountBalance = fixture.save(AccountBalance.builder()
            .accountId(account.getId())
            .referenceDate(Instant.now())
            .balanceType("interimAvailable")
            .amount(MonetaryAmount.of("GBP", 123.45))
            .build());

        // then: the result contains those accounts for the given user-consent ID
        assertNotNull(accountBalance);
        assertNotNull(accountBalance.getAccountId());
    }

    @Test
    public void testMostRecent() {
        // given: a user-consent
        UserConsent consent = createUserConsent();

        // and: linked account
        Account account = createAccounts(consent, 1).getFirst();

        Instant now = Instant.now();

        // and: the account has several account balance records
        List<AccountBalance> accountBalances = List.of(
            AccountBalance.builder()
                .accountId(account.getId())
                .referenceDate(Instant.now().minus(Duration.ofDays(2)))
                .balanceType("interimAvailable")
                .amount(MonetaryAmount.of("GBP", 123.45))
                .build(),
            AccountBalance.builder()
                .accountId(account.getId())
                .referenceDate(Instant.now().minus(Duration.ofDays(2)))
                .balanceType("expected")
                .amount(MonetaryAmount.of("GBP", 223.45))
                .build(),

            AccountBalance.builder()
                .accountId(account.getId())
                .referenceDate(now)
                .balanceType("interimAvailable")
                .amount(MonetaryAmount.of("GBP", 333.45))
                .build(),
            AccountBalance.builder()
                .accountId(account.getId())
                .referenceDate(now)
                .balanceType("expected")
                .amount(MonetaryAmount.of("GBP", 443.45))
                .build()
        );
        fixture.saveAll(accountBalances);

        // when: the repository is called
        List<AccountBalance> mostRecent = fixture.findMostRecentByAccountId(account.getId())
            .map(balance -> fixture.listByReferenceDate(account.getId(), balance.getReferenceDate()))
            .orElse(List.of());

        // then: the most records are returned
        assertFalse(mostRecent.isEmpty());
        assertEquals(2, mostRecent.size());

        // and: the records are as expected
        accountBalances.subList(2, 4).forEach(expected -> {
            AccountBalance actual = mostRecent.stream()
                .filter(result -> result.getId().equals(expected.getId()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getAccountId(), actual.getAccountId());
            assertEquals(expected.getDateCreated(), actual.getDateCreated());
            assertEquals(expected.getReferenceDate(), actual.getReferenceDate());
            assertEquals(expected.getAmount(), actual.getAmount());
            assertEquals(expected.getBalanceType(), actual.getBalanceType());
        });
    }

    private UserConsent createUserConsent() {
        return userConsentRepository.save(UserConsent.builder()
            .provider(RailProvider.NORDIGEN)
            .reference(UUID.randomUUID().toString())
            .userId(UUID.randomUUID())
            .institutionId(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .agreementExpires(Instant.now().plusSeconds(1000))
            .maxHistory(80)
            .status(ConsentStatus.GIVEN)
            .build());
    }

    private List<Account> createAccounts(UserConsent userConsent, int count) {
        List<Account> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(accountRepository.save(Account.builder()
                .userConsentId(userConsent.getId())
                .userId(userConsent.getUserId())
                .institutionId(userConsent.getInstitutionId())
                .railAccountId(UUID.randomUUID().toString())
                .build()));
        }
        accountRepository.flush();
        return result;
    }
}
