package com.hillayes.rail.utils;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import org.apache.commons.lang3.RandomUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestApiData {
    public static RailProviderApi mockRailProviderApi(RailProvider railProvider) {
        RailProviderApi result = mock(RailProviderApi.class);
        when(result.isFor(railProvider)).thenReturn(true);
        when(result.getProviderId()).thenReturn(railProvider);
        return result;
    }

    public static RailInstitution mockInstitution() {
        return mockInstitution(null);
    }

    public static RailInstitution mockInstitution(Consumer<RailInstitution.RailInstitutionBuilder> modifier) {
        RailInstitution.RailInstitutionBuilder builder = RailInstitution.builder()
            .id(UUID.randomUUID().toString())
            .provider(RailProvider.NORDIGEN)
            .name(insecure().nextAlphanumeric(20))
            .countries(List.of("GB"))
            .logo("https://example.com/logo.png")
            .paymentsEnabled(true)
            .transactionTotalDays(RandomUtils.insecure().randomInt(100, 900));

        if (modifier != null) modifier.accept(builder);
        return builder.build();
    }

    public static RailAgreement mockAgreement() {
        return mockAgreement(insecure().nextAlphanumeric(20));
    }

    public static RailAgreement mockAgreement(String id) {
        return mockAgreement(id, AgreementStatus.GIVEN);
    }

    public static RailAgreement mockAgreement(String id, AgreementStatus status) {
        return RailAgreement.builder()
            .id(id)
            .status(status)
            .dateExpires(Instant.now().plus(Duration.ofDays(RandomUtils.insecure().randomInt())))
            .dateGiven(Instant.now().minus(Duration.ofDays(RandomUtils.insecure().randomInt())))
            .dateCreated(Instant.now().minus(Duration.ofDays(RandomUtils.insecure().randomInt())))
            .maxHistory(270)
            .institutionId(insecure().nextAlphanumeric(20))
            .accountIds(List.of(
                insecure().nextAlphanumeric(20),
                insecure().nextAlphanumeric(20)
            ))
            .build();
    }

    public static RailAccount mockAccount() {
        return mockAccount(RailAccountStatus.READY);
    }

    public static RailAccount mockAccount(RailAccountStatus status) {
        return RailAccount.builder()
            .id(insecure().nextAlphanumeric(20))
            .status(status)
            .ownerName(insecure().nextAlphanumeric(10))
            .iban(insecure().nextAlphanumeric(15))
            .name(insecure().nextAlphanumeric(20))
            .accountType(insecure().nextAlphanumeric(6))
            .currency(Currency.getInstance("GBP"))
            .balance(RailBalance.builder()
                .amount(MonetaryAmount.of("GBP", RandomUtils.insecure().randomDouble()))
                .type(insecure().nextAlphanumeric(5))
                .dateTime(Instant.now())
                .build())
            .build();

    }
    public static RailBalance mockBalance() {
        return RailBalance.builder()
            .amount(MonetaryAmount.of("GBP", RandomUtils.insecure().randomDouble()))
            .dateTime(Instant.now().minus(Duration.ofDays(RandomUtils.insecure().randomInt())))
            .type(insecure().nextAlphanumeric(5))
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
            .id(insecure().nextAlphanumeric(20))
            .originalTransactionId(insecure().nextAlphanumeric(20))
            .amount(MonetaryAmount.of("GBP", RandomUtils.insecure().randomDouble()))
            .dateBooked(Instant.now().minus(Duration.ofDays(RandomUtils.insecure().randomInt())))
            .dateValued(Instant.now().minus(Duration.ofDays(RandomUtils.insecure().randomInt())))
            .description(insecure().nextAlphanumeric(20))
            .reference(insecure().nextAlphanumeric(15))
            .creditor(insecure().nextAlphanumeric(20))
            .build();
    }
}
