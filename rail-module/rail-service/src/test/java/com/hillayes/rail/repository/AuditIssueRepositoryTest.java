package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.domain.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hillayes.rail.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestTransaction
public class AuditIssueRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    @Inject
    AuditReportConfigRepository auditReportConfigRepository;

    @Inject
    AuditIssueRepository fixture;

    @Test
    public void testListTransactionIds() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user has an account with transactions
        List<AccountTransaction> transactions = mockTransactions(userId);

        // and: a collection audit report configs
        List<AuditReportConfig> reportConfigs = IntStream.range(0, 5)
            .mapToObj(i -> auditReportConfigRepository.save(mockAuditReportConfig(userId)))
            .toList();

        // and: each report config has a list of audit issues
        Map<UUID, List<AuditIssue>> reportIssues = new HashMap<>();
        reportConfigs.forEach(reportConfig ->
            reportIssues.put(reportConfig.getId(), transactions.stream()
                .map(transaction -> fixture.save(
                    mockAuditIssue(userId, reportConfig.getId(), i -> i.transactionId(transaction.getId())))
                ).toList()
            )
        );

        // make sure that the issues have been saved and cache is cleared
        fixture.flush();
        fixture.getEntityManager().clear();

        reportIssues.forEach((configId, issues) -> {
            // when: we retrieve the issues for each report config
            Set<UUID> transactionIds = fixture.listTransactionIds(configId);

            // then: the result contains all issues for the report config
            assertEquals(issues.size(), transactionIds.size());
            issues.forEach(expected ->
                assertTrue(transactionIds.contains(expected.getTransactionId()))
            );
        });
    }

    @Test
    public void testFindByUserId() {
        // given: a collection of user identities
        List<UUID> userIds = IntStream.range(0, 3).mapToObj(i -> UUID.randomUUID()).toList();

        // and: each user has an account with transactions
        Map<UUID, List<AccountTransaction>> userTransactions = userIds.stream()
            .map(this::mockTransactions)
            .collect(Collectors.toMap(transactions -> transactions.getFirst().getUserId(), transactions -> transactions));

        // and: each user an audit report config
        Map<UUID, AuditReportConfig> reportConfigs = userIds.stream()
            .map(userId -> auditReportConfigRepository.save(mockAuditReportConfig(userId)))
            .collect(Collectors.toMap(AuditReportConfig::getUserId, reportConfig -> reportConfig));

        // and: each user has a list of audit issues for their report config
        Map<UUID, List<AuditIssue>> reportIssues = reportConfigs.entrySet().stream()
            .map(entry -> userTransactions.get(entry.getKey()).stream()
                .map(transaction -> fixture.save(
                    mockAuditIssue(transaction.getUserId(), entry.getValue().getId(), i -> i.transactionId(transaction.getId())))
                ).toList()
            )
            .collect(Collectors.toMap(issues -> issues.getFirst().getUserId(), issues -> issues));

        // make sure that the issues have been saved and cache is cleared
        fixture.flush();
        fixture.getEntityManager().clear();

        reportIssues.forEach((userId, expectedIssues) -> {
            // when: we retrieve the issues for each user
            Page<AuditIssue> result = fixture.findByUserId(userId, 0, 100);

            // then: the result contains all issues for the report config
            assertEquals(expectedIssues.size(), result.getContentSize());
            expectedIssues.forEach(expected ->
                assertTrue(result.getContent().contains(expected))
            );
        });
    }

    @Test
    public void testFindByConfigId() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user has an account with transactions
        List<AccountTransaction> transactions = mockTransactions(userId);

        // and: a collection audit report configs
        List<AuditReportConfig> reportConfigs = IntStream.range(0, 5)
            .mapToObj(i -> auditReportConfigRepository.save(mockAuditReportConfig(userId)))
            .toList();

        // and: each report config has a list of audit issues
        Map<UUID, List<AuditIssue>> reportIssues = new HashMap<>();
        reportConfigs.forEach(reportConfig ->
            reportIssues.put(reportConfig.getId(), transactions.stream()
                .map(transaction -> fixture.save(
                    mockAuditIssue(userId, reportConfig.getId(), i -> i.transactionId(transaction.getId())))
                ).toList()
            )
        );

        // make sure that the issues have been saved and cache is cleared
        fixture.flush();
        fixture.getEntityManager().clear();

        reportIssues.forEach((configId, issues) -> {
            // when: we retrieve the issues for each report config
            Page<AuditIssue> result = fixture.findByConfigId(configId, null, 0, 100);

            // then: the result contains all issues for the report config
            assertEquals(issues.size(), result.getContentSize());
            issues.forEach(expected ->
                assertTrue(result.getContent().contains(expected))
            );
        });
    }

    @Test
    public void testGetByReportAndTransaction() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user has an account with transactions
        List<AccountTransaction> transactions = mockTransactions(userId);

        // and: a collection audit report configs
        List<AuditReportConfig> reportConfigs = IntStream.range(0, 5)
            .mapToObj(i -> auditReportConfigRepository.save(mockAuditReportConfig(userId)))
            .toList();

        // and: each report config has a list of audit issues
        Map<UUID, List<AuditIssue>> reportIssues = new HashMap<>();
        reportConfigs.forEach(reportConfig ->
            reportIssues.put(reportConfig.getId(), transactions.stream()
                .map(transaction -> fixture.save(
                    mockAuditIssue(userId, reportConfig.getId(), i -> i.transactionId(transaction.getId())))
                ).toList()
            )
        );

        // make sure that the issues have been saved and cache is cleared
        fixture.flush();
        fixture.getEntityManager().clear();

        reportIssues.forEach((configId, issues) -> {
            issues.forEach(issue -> {
                // when: we retrieve an issue by report config and transaction
                Optional<AuditIssue> result = fixture.getByReportAndTransaction(issue.getReportConfigId(), issue.getTransactionId());

                // then: the identified issue is returned
                assertTrue(result.isPresent());
                assertEquals(issue, result.get());
            });
        });
    }

    private List<AccountTransaction> mockTransactions(UUID userId) {
        // and: the user has an account with transactions
        UserConsent userConsent = userConsentRepository.save(mockUserConsent(userId, c -> c.id(null)));
        Account account = accountRepository.save(mockAccount(userId, a -> a.id(null).userConsentId(userConsent.getId())));
        return IntStream.range(0, 5)
            .mapToObj(i -> accountTransactionRepository.save(mockAccountTransaction(account, t -> t.id(null))))
            .toList();
    }
}
