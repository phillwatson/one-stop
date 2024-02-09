package com.hillayes.rail.utils;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.api.domain.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextDouble;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class TestApiData {
    public static RailInstitution mockInstitution() {
        return RailInstitution.builder()
            .id(UUID.randomUUID().toString())
            .provider(RailProvider.NORDIGEN)
            .name(randomAlphanumeric(20))
            .countries(List.of("GB"))
            .logo("https://example.com/logo.png")
            .paymentsEnabled(true)
            .transactionTotalDays(nextInt(100, 900))
            .build();
    }

    public static RailAgreement mockAgreement() {
        return mockAgreement(randomAlphanumeric(20));
    }

    public static RailAgreement mockAgreement(String id) {
        return mockAgreement(id, AgreementStatus.GIVEN);
    }

    public static RailAgreement mockAgreement(String id, AgreementStatus status) {
        return RailAgreement.builder()
            .id(id)
            .status(status)
            .dateExpires(Instant.now().plus(Duration.ofDays(nextInt())))
            .dateGiven(Instant.now().minus(Duration.ofDays(nextInt())))
            .dateCreated(Instant.now().minus(Duration.ofDays(nextInt())))
            .maxHistory(270)
            .institutionId(randomAlphanumeric(20))
            .accountIds(List.of(
                randomAlphanumeric(20),
                randomAlphanumeric(20)
            ))
            .build();
    }

    public static RailAccount mockAccount() {
        return mockAccount(AccountStatus.READY);
    }

    public static RailAccount mockAccount(AccountStatus status) {
        return RailAccount.builder()
            .id(randomAlphanumeric(20))
            .status(status)
            .ownerName(randomAlphanumeric(10))
            .iban(randomAlphanumeric(15))
            .name(randomAlphanumeric(20))
            .accountType(randomAlphanumeric(6))
            .currency(Currency.getInstance("GBP"))
            .balance(RailBalance.builder()
                .amount(MonetaryAmount.of("GBP", nextDouble()))
                .type(randomAlphanumeric(5))
                .build())
            .build();

    }
    public static RailBalance mockBalance() {
        return RailBalance.builder()
            .amount(MonetaryAmount.of("GBP", nextDouble()))
            .dateTime(Instant.now().minus(Duration.ofDays(nextInt())))
            .type(randomAlphanumeric(5))
            .build();
    }

    public static List<RailTransaction> mockTransactionList(int count) {
        return (count <= 0)
            ? List.of()
            : Stream.generate(TestApiData::mockTransaction)
                .limit(count)
                .toList();
    }

    public static RailTransaction mockTransaction() {
        return RailTransaction.builder()
            .id(randomAlphanumeric(20))
            .originalTransactionId(randomAlphanumeric(20))
            .amount(MonetaryAmount.of("GBP", nextDouble()))
            .dateBooked(Instant.now().minus(Duration.ofDays(nextInt())))
            .dateValued(Instant.now().minus(Duration.ofDays(nextInt())))
            .description(randomAlphanumeric(20))
            .reference(randomAlphanumeric(15))
            .creditor(randomAlphanumeric(20))
            .build();
    }
}
