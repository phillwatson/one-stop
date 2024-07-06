package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class CategoryService {
    @Inject
    CategoryRepository categoryRepository;

    /**
     * Returns the selected page of categories for the specified user; in category name order.
     *
     * @param userId The user ID.
     * @param page The zero-based, page number.
     * @param pageSize The max number of categories per page.
     * @return The selected page of categories.
     */
    public Page<Category> getCategories(UUID userId, int page, int pageSize) {
        log.info("Listing categories [userId: {}]", userId);
        Page<Category> result = categoryRepository.findByUserId(userId, page, pageSize);

        log.debug("Listing categories [userId: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
            userId, page, pageSize, result.getContentSize(), result.getTotalCount());
        return result;
    }

    /**
     * Deletes all categories for the specified user.
     *
     * @param userId The user ID.
     */
    public void deleteAllCategories(UUID userId) {
        log.info("Deleting all categories for user [userId: {}]", userId);
        categoryRepository.deleteByUserId(userId);
    }

    /**
     * Returns the selectors for the identified category and account.
     *
     * @param categoryId The category ID.
     * @return The category selectors.
     */
    public Collection<CategorySelector> getCategorySelectors(UUID categoryId, UUID accountId) {
        log.info("Listing category selectors [categoryId: {}]", categoryId);
        return categoryRepository.findByIdOptional(categoryId)
            .map(Category::getSelectors)
            .orElseThrow(() -> new NotFoundException("Category", categoryId))
            .stream()
            .filter(selector -> selector.getAccountId().equals(accountId))
            .toList();
    }
}
