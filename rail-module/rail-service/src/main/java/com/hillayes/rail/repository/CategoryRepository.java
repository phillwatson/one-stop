package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.Category;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CategoryRepository extends RepositoryBase<Category, UUID> {
    public void deleteByUserId(UUID userId) {
        delete("userId", userId);
    }

    public Page<Category> findByUserId(UUID userId, int page, int pageSize) {
        return pageAll("userId = :userId", page, pageSize,
            OrderBy.by("name"), Map.of("userId", userId));
    }

    /**
     * select coalesce (c."name", 'UNCATEGORISED') as category, c.id, count(*), sum(t.amount) / 100 as value
     * from rails.account_transaction t
     * left join rails.category_selector cs on cs.account_id = t.account_id and
     *   (cs.info_contains is null or t.additional_information like concat('%', cs.info_contains, '%')) and
     *   (cs.ref_contains is null or t.reference like concat('%', cs.ref_contains, '%')) and
     *   (cs.creditor_contains is null or t.creditor_name like concat('%', cs.creditor_contains, '%'))
     * left join rails.category c on c.user_id = t.user_id and c.id = cs.category_id
     * where t.user_id = 'dd104a91-ba98-4202-b70d-10727264f67b'
     * group by 1, 2 order by 1;
     */
}
