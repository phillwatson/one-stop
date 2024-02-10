package com.hillayes.rail.utils;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;

import java.time.*;
import java.util.Currency;
import java.util.UUID;
import java.util.function.Function;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextDouble;

public class TestData {
    public static UserConsent mockUserConsent(UUID userId) {
        return mockUserConsent(userId, consent -> consent);
    }

    public static UserConsent mockUserConsent(UUID userId, Function<UserConsent, UserConsent> modifier) {
        UserConsent result = UserConsent.builder()
            .id(UUID.randomUUID())
            .provider(RailProvider.NORDIGEN)
            .userId(userId)
            .status(ConsentStatus.GIVEN)
            .reference(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .agreementExpires(Instant.now().plus(Duration.ofDays(90)))
            .maxHistory(270)
            .dateGiven(Instant.now().minusSeconds(2))
            .build();

        return modifier.apply(result);
    }

    public static Account mockAccount(UUID userId, UUID userConsentId) {
        return Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .userConsentId(userConsentId)
            .institutionId(UUID.randomUUID().toString())
            .railAccountId(UUID.randomUUID().toString())
            .accountName(randomAlphanumeric(20))
            .currency(Currency.getInstance("GBP"))
            .iban(randomAlphanumeric(20))
            .accountType("CHEK")
            .build();
    }

    public static AccountBalance mockAccountBalance(Account account, String balanceType) {
        return AccountBalance.builder()
            .id(UUID.randomUUID())
            .accountId(account.getId())
            .balanceType(balanceType)
            .amount(MonetaryAmount.of("GBP", nextDouble()))
            .referenceDate(Instant.now().minus(Duration.ofDays(1)))
            .dateCreated(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC))
            .build();
    }
}
