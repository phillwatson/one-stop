package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

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
        Account account = createAccounts(consent, 1).get(0);

        // when: an account balance is saved
        AccountBalance accountBalance = fixture.save(AccountBalance.builder()
                .accountId(account.getId())
                .referenceDate(LocalDate.now())
                .balanceType("CHK")
                .currencyCode("GBP")
                .amount(123.45)
            .build());

        // then: the result contains those accounts for the given user-consent ID
        assertNotNull(accountBalance);
        assertNotNull(accountBalance.getAccountId());
    }

    private UserConsent createUserConsent() {
        return userConsentRepository.save(UserConsent.builder()
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
