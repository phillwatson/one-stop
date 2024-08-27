package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.CategoryGroup;
import com.hillayes.rail.domain.CategoryStatistics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class CategoryGroupRepository extends RepositoryBase<CategoryGroup, UUID> {
    @Inject
    ServiceConfiguration serviceConfiguration;

    public void deleteByUserId(UUID userId) {
        delete("userId", userId);
    }

    public Page<CategoryGroup> findByUserId(UUID userId, int page, int pageSize) {
        return pageAll("userId = :userId", page, pageSize,
            OrderBy.by("name"), Map.of("userId", userId));
    }

    public Optional<CategoryGroup> findByUserAndName(UUID userId, String name) {
        return findFirst("userId = :userId and name = :name",
            Map.of("userId", userId, "name", name));
    }

    /**
     * Get the category statistics for the identified category group and date range.
     * The result will be a list of {@link CategoryStatistics} instances, each
     * representing a category, the number of transactions and the total value of
     * transactions in that category. Any transactions that do match a category
     * will be included in the "uncategorised" category.
     *
     * @param group The group for which to get statistics.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (exclusive).
     * @return The list of category statistics.
     */
    public List<CategoryStatistics> getStatistics(CategoryGroup group, Instant startDate, Instant endDate) {
        String defaultName = serviceConfiguration.categories().uncategorisedName();
        String defaultColour = serviceConfiguration.categories().defaultColour();

        // combine the results of all the queries into a single list
        return STATS_QUERIES.stream()
            .map(sql -> getEntityManager().createNativeQuery(sql, CategoryStatistics.class)
                .setParameter("userId", group.getUserId())
                .setParameter("groupId", group.getId())
                .setParameter("groupName", group.getName())
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("defaultName", defaultName)
                .setParameter("defaultColour", defaultColour)
                .getResultList()
            )
            .reduce(new ArrayList<>(), (list, stats) -> {
                list.addAll(stats);
                return list;
            });
    }

    // returns the stats for those transactions that fall within a category
    private static final String SQL_CATEGORISED_STATS = "select " +
        ":groupId as group_id, :groupName as group_name, " +
        "coalesce (c.name, :defaultName) as category, c.id, c.description, coalesce (c.colour, :defaultColour) as colour, " +
        "count(*) as count, sum(t.amount) / 100 as total, " +
        "sum(case when t.amount > 0 then t.amount else 0 end) / 100 as credit, " +
        "sum(case when t.amount < 0 then abs(t.amount) else 0 end) / 100 as debit " +
        "from rails.account_transaction t " +
        "inner join rails.category c on c.group_id = :groupId " +
        "inner join rails.category_selector cs on " +
        "  cs.category_id = c.id and" +
        "  cs.account_id = t.account_id and " +
        "  (cs.info_contains is null or t.additional_information like concat('%', cs.info_contains, '%')) and " +
        "  (cs.ref_contains is null or t.reference like concat('%', cs.ref_contains, '%')) and " +
        "  (cs.creditor_contains is null or t.creditor_name like concat('%', cs.creditor_contains, '%')) " +
        "where t.user_id = :userId " +
        "and t.booking_datetime >= :startDate " +
        "and t.booking_datetime < :endDate " +
        "group by 1, 2, 3, 4 order by 1";

    // returns the stats for those transactions that DO NOT fall within a category
    private static final String SQL_UNCATEGORISED_STATS = "select " +
        ":groupId as group_id, :groupName as group_name, " +
        ":defaultName as category, null, null, :defaultColour as colour, " +
        "count(*) as count, sum(t.amount) / 100 as total, " +
        "sum(case when t.amount > 0 then t.amount else 0 end) / 100 as credit, " +
        "sum(case when t.amount < 0 then abs(t.amount) else 0 end) / 100 as debit " +
        "from rails.account_transaction t " +
        "where t.user_id = :userId " +
        "and t.booking_datetime >= :startDate " +
        "and t.booking_datetime < :endDate " +
        "and not exists ( " +
        "  select 1 from rails.category_selector cs" +
        "  inner join rails.category c on c.group_id = :groupId and " +
        "    cs.category_id = c.id and " +
        "    cs.account_id = t.account_id and " +
        "    (cs.info_contains is null or t.additional_information like concat('%', cs.info_contains, '%')) and " +
        "    (cs.ref_contains is null or t.reference like concat('%', cs.ref_contains, '%')) and " +
        "    (cs.creditor_contains is null or t.creditor_name like concat('%', cs.creditor_contains, '%')) " +
        ")" +
        "group by 1, 2, 3, 4 order by 1";

    private static final List<String> STATS_QUERIES = List.of(
        SQL_CATEGORISED_STATS,
        SQL_UNCATEGORISED_STATS
    );
}
