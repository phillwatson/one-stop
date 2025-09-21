package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

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

    @Test
    public void testGetByUserId() {
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
            Page<Account> actual = fixture.findByUserId(consent.getUserId(), 0, 20);

            // then: the result contains those accounts for the given user-consent ID
            assertEquals(expected.size(), actual.getContentSize());
            expected.forEach(account -> assertTrue(actual.getContent().contains(account)));
        });
    }

    @Test
    public void testFindByRailAccountId() {
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

        accounts.forEach((consent, expected) -> {
            expected.forEach(account -> {
                // when: the accounts are retrieved by rail-account ID
                Optional<Account> actual = fixture.findByRailAccountId(account.getRailAccountId());

                // then: the result is the expected account
                assertTrue(actual.isPresent());
                assertEquals(account.getId(), actual.get().getId());
            });
        });
    }

    @Test
    public void testFindByIban() {
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

        accounts.forEach((consent, expected) -> {
            expected.forEach(account -> {
                // when: the accounts are retrieved by IBAN
                Optional<Account> actual = fixture.findByIban(consent.getUserId(), account.getIban());

                // then: the result is the expected account
                assertTrue(actual.isPresent());
                assertEquals(account.getId(), actual.get().getId());
            });
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
            result.add(fixture.save(Account.builder()
                .userConsentId(userConsent.getId())
                .userId(userConsent.getUserId())
                .institutionId(userConsent.getInstitutionId())
                .railAccountId(UUID.randomUUID().toString())
                .iban(UUID.randomUUID().toString())
                .build()));
        }
        fixture.flush();
        return result;
    }
}
