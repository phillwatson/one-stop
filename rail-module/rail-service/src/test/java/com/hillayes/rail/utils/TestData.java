package com.hillayes.rail.utils;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.Currency;
import java.util.UUID;
import java.util.function.Consumer;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextLong;

public class TestData {
    public static UserConsent mockUserConsent(UUID userId) {
        return mockUserConsent(userId, null);
    }

    public static UserConsent mockUserConsent(UUID userId, Consumer<UserConsent.UserConsentBuilder> modifier) {
        UserConsent.UserConsentBuilder builder = UserConsent.builder()
            .id(UUID.randomUUID())
            .provider(RailProvider.NORDIGEN)
            .userId(userId)
            .status(ConsentStatus.GIVEN)
            .reference(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .agreementExpires(Instant.now().plus(Duration.ofDays(90)))
            .maxHistory(270)
            .dateGiven(Instant.now().minusSeconds(2));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }

    public static Account mockAccount(UUID userId, UUID userConsentId) {
        return mockAccount(userId, (a) -> a.userConsentId(userConsentId));
    }

    public static Account mockAccount(UUID userId, Consumer<Account.Builder> modifier) {
        Account.Builder builder = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .userConsentId(UUID.randomUUID())
            .institutionId(UUID.randomUUID().toString())
            .railAccountId(UUID.randomUUID().toString())
            .accountName(randomAlphanumeric(20))
            .currency(Currency.getInstance("GBP"))
            .iban(randomAlphanumeric(20))
            .accountType("CHEK");

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }

    public static AccountBalance mockAccountBalance(Account account) {
        return mockAccountBalance(account, null);
    }

    public static AccountBalance mockAccountBalance(Account account,
                                                    Consumer<AccountBalance.Builder> modifier) {
        AccountBalance.Builder builder = AccountBalance.builder()
            .id(UUID.randomUUID())
            .accountId(account.getId())
            .balanceType(randomAlphanumeric(30))
            .amount(MonetaryAmount.of("GBP", nextLong(0, 20000)))
            .referenceDate(Instant.now().minus(Duration.ofDays(1)))
            .dateCreated(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }

    public static AccountTransaction mockAccountTransaction(Account account) {
        return mockAccountTransaction(account, null);
    }

    public static AccountTransaction mockAccountTransaction(Account account,
                                                            Consumer<AccountTransaction.Builder> modifier) {
        return mockAccountTransaction(c -> {
            c.accountId(account.getId());
            c.userId(account.getUserId());
            if (modifier != null) {
                modifier.accept(c);
            }
        });
    }

    public static AccountTransaction mockAccountTransaction(Consumer<AccountTransaction.Builder> modifier) {
        AccountTransaction.Builder builder = AccountTransaction.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .accountId(UUID.randomUUID())
            .bookingDateTime(Instant.now().minus(Duration.ofDays(1)))
            .valueDateTime(Instant.now().minus(Duration.ofDays(1)))
            .amount(MonetaryAmount.of("GBP", nextLong(0, 200000)))
            .reference(randomAlphanumeric(30))
            .additionalInformation(randomAlphanumeric(30))
            .creditorName(randomAlphanumeric(30))
            .transactionId(randomAlphanumeric(30))
            .internalTransactionId(randomAlphanumeric(30));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }

    public static CategoryStatistics mockCategoryStatistics(CategoryGroup group,
                                                            String categoryName,
                                                            int count, double total, double credit, double debit) {
        return new CategoryStatistics(group.getId(), group.getName(), categoryName, UUID.randomUUID(),
            randomAlphanumeric(30), "#345678",
            count, BigDecimal.valueOf(total), BigDecimal.valueOf(credit), BigDecimal.valueOf(debit));
    }

    public static AuditReportConfig mockAuditReportConfig(UUID userId) {
        return mockAuditReportConfig(userId, (b) -> {});
    }

    public static AuditReportConfig mockAuditReportConfig(UUID userId,
                                                          Consumer<AuditReportConfig.Builder> modifier) {
        AuditReportConfig.Builder builder = AuditReportConfig.builder()
            .disabled(false)
            .userId(userId)
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
            .reportSource(AuditReportConfig.ReportSource.CATEGORY_GROUP)
            .reportSourceId(UUID.randomUUID())
            .uncategorisedIncluded(true)
            .templateName(randomAlphanumeric(30));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }
}
