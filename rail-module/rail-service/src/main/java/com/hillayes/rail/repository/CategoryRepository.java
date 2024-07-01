package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.Category;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class CategoryRepository extends RepositoryBase<Category, UUID> {
}
