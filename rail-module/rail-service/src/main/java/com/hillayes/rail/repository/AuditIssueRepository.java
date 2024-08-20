package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditIssueSummary;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class AuditIssueRepository extends RepositoryBase<AuditIssue, UUID> {
    public Page<AuditIssue> findByUserId(UUID userId, int page, int pageSize) {
        return pageAll("userId = :userId", page, pageSize,
            OrderBy.by("bookingDateTime", OrderBy.Direction.Descending),
            Map.of("userId", userId));
    }

    public Page<AuditIssue> findByConfigId(UUID configId, Boolean acknowledged, int page, int pageSize) {
        if (acknowledged != null) {
            String nullPredicate = (acknowledged == Boolean.TRUE) ? "is not null" : "is null";
            return pageAll("reportConfigId = :reportConfigId and acknowledgedDateTime " + nullPredicate, page, pageSize,
                OrderBy.by("bookingDateTime", OrderBy.Direction.Descending),
                Map.of("reportConfigId", configId));
        }

        return pageAll("reportConfigId = :reportConfigId", page, pageSize,
            OrderBy.by("bookingDateTime", OrderBy.Direction.Descending),
            Map.of("reportConfigId", configId));
    }

    public List<AuditIssueSummary> getIssueSummaries(UUID userId) {
        return getEntityManager().createNativeQuery(
                "select r.id, r.name, " +
                    "count(ai.id) as total_count, " +
                    "sum(case when ai.acknowledged_datetime is null then 0 else 1 end) as acknowledged_count " +
                    "from rails.audit_report_config r " +
                    "join rails.audit_issue ai on ai.report_config_id = r.id " +
                    "where r.user_id = :userId " +
                    "group by r.id, r.name;", AuditIssueSummary.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    /**
     * Deletes all audit issues for the identified user.
     * @param userId the user identity.
     */
    public void deleteByUser(UUID userId) {
        delete("userId", userId);
    }

    /**
     * Deletes all issues for the identified user audit report configuration.
     * @param reportConfigId the identified user audit report configuration.
     */
    public void deleteByReportConfig(UUID reportConfigId) {
        delete("reportConfigId", reportConfigId);
    }

    /**
     * Deletes all issues for the identified user audit report configuration that
     * have been acknowledged for longer than the given duration.
     * @param reportConfigId the identified user audit report configuration.
     * @param ackDuration the duration after which acknowledged issues will be deleted.
     */
    public void deleteAcknowledged(UUID reportConfigId,
                                   Duration ackDuration) {
        Instant timeout = Instant.now().minus(ackDuration);
        delete("reportConfigId = :reportConfigId AND acknowledgedDateTime < :timeout",
            Map.of("reportConfigId", reportConfigId, "timeout", timeout));
    }

    /**
     * Returns the identities of transactions for which issues have been found
     * by the identified user audit report configuration.
     * @param reportConfigId the identified user audit report configuration.
     * @return the identities of transactions for which issues have been found.
     */
    public Set<UUID> listTransactionIds(UUID reportConfigId) {
        return new HashSet<>(
            getEntityManager().createNativeQuery(
                    "SELECT i.transaction_id FROM rails.audit_issue i" +
                        " WHERE i.report_config_id = :reportConfigId", UUID.class)
                .setParameter("reportConfigId", reportConfigId)
                .getResultList()
        );
    }

    /**
     * Returns any issue related to the identified user audit report configuration
     * for the identified transaction.
     * @param reportConfigId the identified user audit report configuration.
     * @param transactionId the identified transaction.
     */
    public Optional<AuditIssue> getByReportAndTransaction(UUID reportConfigId, UUID transactionId) {
        return find("reportConfigId = :reportConfigId and transactionId = :transactionId",
            Map.of("reportConfigId", reportConfigId, "transactionId", transactionId))
            .stream()
            .findFirst();
    }
}
