package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AuditIssue;
import jakarta.enterprise.context.ApplicationScoped;

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
            return pageAll("reportConfigId = :reportConfigId and acknowledged = :acknowledged", page, pageSize,
                OrderBy.by("bookingDateTime", OrderBy.Direction.Descending),
                Map.of("reportConfigId", configId, "acknowledged", acknowledged));
        }

        return pageAll("reportConfigId = :reportConfigId", page, pageSize,
            OrderBy.by("bookingDateTime", OrderBy.Direction.Descending),
            Map.of("reportConfigId", configId));
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
        find("reportConfigId", reportConfigId)
            .stream()
            .forEach(this::delete);
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
