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
    public Page<Category> findByGroupId(UUID groupId, int page, int pageSize) {
        return pageAll("group.id = :groupId", page, pageSize,
            OrderBy.by("name"), Map.of("groupId", groupId));
    }
}
