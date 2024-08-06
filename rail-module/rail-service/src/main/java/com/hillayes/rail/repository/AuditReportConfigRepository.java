package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.domain.CategoryGroup;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuditReportConfigRepository extends RepositoryBase<AuditReportConfig, UUID> {
    public Optional<AuditReportConfig> findByUserAndName(UUID userId, String name) {
        return findFirst("userId = :userId and name = :name",
            Map.of("userId", userId, "name", name));
    }

    public Page<AuditReportConfig> findByUserId(UUID userId, int page, int pageSize) {
        return pageAll("userId = :userId", page, pageSize,
            OrderBy.by("name"), Map.of("userId", userId));
    }

    /**
     * Deletes all reports that use the identified entity as their transaction source.
     * The source identity can be for an account, category group or category.
     * @param reportSourceId the identified transaction source.
     */
    public void deleteByReportSource(UUID reportSourceId) {
        find("reportSourceId", reportSourceId)
            .stream()
            .forEach(this::delete);
    }

    /**
     * Returns the list of user IDs that have audit reports.
     */
    public List<UUID> listUserIds() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT userId FROM audit_report_config", UUID.class)
            .getResultList();
    }
}
