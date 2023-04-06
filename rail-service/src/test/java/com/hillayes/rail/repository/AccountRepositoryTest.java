package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestTransaction
public class AccountRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository fixture;

    @Test
    public void testGetByUserConsentId() {
        // given: a user-consent
        List<UserConsent> consents = List.of(
            createUserConsent(),
            createUserConsent()
        );

        // and: linked accounts
        Map<UserConsent, List<Account>> accounts = new HashMap<>();
        consents.forEach(consent ->
            accounts.put(consent, createAccounts(consent, 2))
        );

        // when: the accounts are retrieved by user-consent ID
        accounts.forEach((consent, expected) -> {
            List<Account> actual = fixture.findByUserConsentId(consent.getId());

            // then: the result contains those accounts for the given user-consent ID
            assertEquals(expected.size(), actual.size());
            expected.forEach(account -> assertTrue(actual.contains(account)));
        });
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
            result.add(fixture.save(Account.builder()
                .userConsentId(userConsent.getId())
                .userId(userConsent.getUserId())
                .institutionId(userConsent.getInstitutionId())
                .railAccountId(UUID.randomUUID().toString())
                .build()));
        }
        fixture.flush();
        return result;
    }
}
