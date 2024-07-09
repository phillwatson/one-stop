package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategoryStatistics;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.errors.CategoryAlreadyExistsException;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class CategoryService {
    @Inject
    CategoryRepository categoryRepository;

    @Inject
    AccountRepository accountRepository;

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

    public Category getCategory(UUID userId, UUID categoryId) {
        log.info("Getting category [userId: {}, categoryId: {}]", userId, categoryId);
        return categoryRepository.findByIdOptional(categoryId)
            .filter(category -> category.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("Category", categoryId));
    }

    public Category createCategory(UUID userId, String name, String description, String colour) {
        log.info("Creating category [userId: {}, category: {}]", userId, name);

        String newName = name.trim();
        categoryRepository.findByUserAndName(userId, newName)
            .ifPresent(category -> { throw new CategoryAlreadyExistsException(category); });

        Category category = Category.builder()
            .userId(userId)
            .name(newName)
            .description(description)
            .colour(colour)
            .build();
        return categoryRepository.save(category);
    }

    public Category updateCategory(UUID userId, UUID categoryId,
                                   String name, String description, String colour) {
        log.info("Updating category [userId: {}, categoryId: {}, category: {}]", userId, categoryId, name);
        Category category = getCategory(userId, categoryId);

        category.setName(name);
        category.setDescription(description);
        category.setColour(colour);
        return categoryRepository.save(category);
    }

    /**
     * Returns the selectors for the identified category and account.
     *
     * @param categoryId The category ID.
     * @return The category selectors.
     */
    public Collection<CategorySelector> getCategorySelectors(UUID userId, UUID categoryId, UUID accountId) {
        log.info("Listing category selectors [userId: {}, categoryId: {}, accountId: {}]", userId, categoryId, accountId);
        Category category = validate(userId, categoryId, accountId);

        return category.getSelectors().stream()
            .filter(selector -> selector.getAccountId().equals(accountId))
            .toList();
    }

    /**
     * Sets the selectors for the identified category and account. Overwrites any existing selectors
     * for the same account.
     *
     * @param userId The user ID to whom the category and account must belong.
     * @param categoryId The category ID.
     * @param accountId The account ID to which the selectors are to be associated.
     * @param selectors The category selectors to be set.
     */
    public void setCategorySelectors(UUID userId, UUID categoryId, UUID accountId,
                                     Collection<CategorySelector> selectors) {
        log.info("Setting category selectors [userId: {}, categoryId: {}, accountId: {}]", userId, categoryId, accountId);
        Category category = validate(userId, categoryId, accountId);

        category.getSelectors().removeIf(selector -> selector.getAccountId().equals(accountId));
        if (selectors != null) {
            selectors.forEach(newSelector -> category.addSelector(accountId, selector -> selector
                .infoContains(newSelector.getInfoContains())
                .refContains(newSelector.getRefContains())
                .creditorContains(newSelector.getCreditorContains())
            ));
        }

        categoryRepository.save(category);
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
     * Generates the category statistics for the specified user and date range.
     * @param userId The user ID.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (exclusive).
     * @return The list of category statistics.
     */
    public List<CategoryStatistics> getStatistics(UUID userId, Instant startDate, Instant endDate) {
        return categoryRepository.getStatistics(userId, startDate, endDate);
    }

    /**
     * Ensures that both the category and account exist and belong to the specified user.
     * @param userId The user ID.
     * @param categoryId The category ID.
     * @param accountId The account ID.
     * @return The identified category.
     */
    private Category validate(UUID userId, UUID categoryId, UUID accountId) {
        Category category = getCategory(userId, categoryId);

        accountRepository.findByIdOptional(accountId)
            .filter(account -> account.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("Account", accountId));

        return category;
    }
}
