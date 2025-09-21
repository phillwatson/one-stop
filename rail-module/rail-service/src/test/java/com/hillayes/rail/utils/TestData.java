package com.hillayes.rail.utils;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;
import java.util.function.Consumer;

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
            .accountName(RandomStringUtils.insecure().nextAlphanumeric(20))
            .currency(Currency.getInstance("GBP"))
            .iban(RandomStringUtils.insecure().nextAlphanumeric(20))
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
            .balanceType(RandomStringUtils.insecure().nextAlphanumeric(30))
            .amount(MonetaryAmount.of("GBP", RandomUtils.insecure().randomLong(0, 20000) - 10000))
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
            .amount(MonetaryAmount.of("GBP", RandomUtils.insecure().randomLong(0, 200000) - 100000))
            .reference(RandomStringUtils.insecure().nextAlphanumeric(30))
            .additionalInformation(RandomStringUtils.insecure().nextAlphanumeric(30))
            .creditorName(RandomStringUtils.insecure().nextAlphanumeric(30))
            .transactionId(RandomStringUtils.insecure().nextAlphanumeric(30))
            .internalTransactionId(RandomStringUtils.insecure().nextAlphanumeric(30));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }

    public static CategoryStatistics mockCategoryStatistics(CategoryGroup group,
                                                            String categoryName,
                                                            int count, double total, double credit, double debit) {
        return new CategoryStatistics(group.getId(), group.getName(), categoryName, UUID.randomUUID(),
            RandomStringUtils.insecure().nextAlphanumeric(30), "#345678",
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
            .name(RandomStringUtils.insecure().nextAlphanumeric(30))
            .description(RandomStringUtils.insecure().nextAlphanumeric(30))
            .reportSource(AuditReportConfig.ReportSource.CATEGORY_GROUP)
            .reportSourceId(UUID.randomUUID())
            .uncategorisedIncluded(true)
            .templateName(RandomStringUtils.insecure().nextAlphanumeric(30));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }

    public static AuditIssueSummary mockAuditIssueSummary(AuditReportConfig reportConfig) {
        long count = RandomUtils.insecure().randomLong(0, 100);
        return AuditIssueSummary.builder()
            .auditConfigId(reportConfig.getId())
            .auditConfigName(reportConfig.getName())
            .totalCount(count)
            .acknowledgedCount(RandomUtils.insecure().randomLong(0, count))
            .build();
    }

    public static AuditIssue mockAuditIssue(UUID userId, UUID reportConfigId) {
        return mockAuditIssue(userId, reportConfigId, (b) -> {});
    }

    public static AuditIssue mockAuditIssue(AuditReportConfig reportConfig,
                                            Consumer<AuditIssue.Builder> modifier) {
        return mockAuditIssue(reportConfig.getUserId(), reportConfig.getId(), modifier);
    }

    public static AuditIssue mockAuditIssue(UUID userId, UUID reportConfigId,
                                            Consumer<AuditIssue.Builder> modifier) {
        AuditIssue.Builder builder = AuditIssue.builder()
            .userId(userId)
            .reportConfigId(reportConfigId)
            .bookingDateTime(Instant.now().minus(Duration.ofDays(2)))
            .transactionId(UUID.randomUUID())
            .acknowledgedDateTime(RandomUtils.insecure().randomBoolean() ? null : Instant.now().minus(Duration.ofDays(1)));

        if (modifier != null) {
            modifier.accept(builder);
        }
        return builder.build();
    }
}
