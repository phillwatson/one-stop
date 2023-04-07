package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        UserConsent consent = userConsentRepository.save(UserConsent.builder()
            .userId(UUID.randomUUID())
            .institutionId(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .agreementExpires(Instant.now().plusSeconds(1000))
            .maxHistory(80)
            .status(ConsentStatus.GIVEN)
            .build());

        // and: a linked account
        Account account = accountRepository.save(Account.builder()
            .userConsentId(consent.getId())
            .userId(consent.getUserId())
            .institutionId(consent.getInstitutionId())
            .railAccountId(UUID.randomUUID().toString())
            .build());

        // and: a list of transactions
        List<AccountTransaction> transactions = new ArrayList<>();
        LocalDate bookingDate = LocalDate.now().minusWeeks(5);
        while (bookingDate.isBefore(LocalDate.now())) {
            transactions.add(AccountTransaction.builder()
                    .userId(account.getUserId())
                .accountId(account.getId())
                .internalTransactionId(UUID.randomUUID().toString())
                .bookingDateTime(bookingDate.atStartOfDay(ZoneOffset.UTC).toInstant())
                .transactionAmount(123.33)
                .transactionCurrencyCode("GBP")
                .build());
            bookingDate = bookingDate.plusWeeks(1);
        }
        fixture.saveAll(transactions);
        fixture.flush();

        // when: the most recent transaction by bookingDate is queried
        PageRequest byBookedDate = PageRequest.of(0, 1, Sort.by("bookingDate").descending());
        Page<AccountTransaction> result = fixture.findByAccountId(account.getId(), byBookedDate);

        // then: the result contains only the most recent transaction
        assertEquals(1, result.getSize());
        assertEquals(transactions.get(transactions.size() - 1), result.getContent().get(0));
    }
}
