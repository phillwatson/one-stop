package com.hillayes.rail.service;

import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategoryGroup;
import com.hillayes.rail.domain.CategoryStatistics;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.errors.CategoryAlreadyExistsException;
import com.hillayes.rail.errors.CategoryGroupAlreadyExistsException;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.CategoryGroupRepository;
import com.hillayes.rail.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    /**
     * Returns the selected page of category groups for the specified user; in name order.
     *
     * @param userId The user ID.
     * @param pageIndex The zero-based, page number.
     * @param pageSize The max number of groups per page.
     * @return The selected page of category groups.
     */
    public Page<CategoryGroup> getCategoryGroups(UUID userId, int pageIndex, int pageSize) {
        log.info("Listing category groups [userId: {}, page: {}, pageSize: {}]", userId, pageIndex, pageSize);
        Page<CategoryGroup> result = categoryGroupRepository.findByUserId(userId, pageIndex, pageSize);

        if (log.isDebugEnabled()) {
            log.debug("Listing category groups [userId: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
                userId, pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        }
        return result;
    }

    public CategoryGroup getCategoryGroup(UUID userId, UUID groupId) {
        log.info("Getting category group [userId: {}, groupId: {}]", userId, groupId);
        return categoryGroupRepository.findByIdOptional(groupId)
            .filter(category -> category.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("CategoryGroup", groupId));
    }

    public CategoryGroup createCategoryGroup(UUID userId, String name, String description) {
        log.info("Creating category group [userId: {}, name: {}]", userId, name);

        String newName = Strings.trimOrNull(name);
        if (newName == null) {
            throw new MissingParameterException("CategoryGroup.name");
        }

        categoryGroupRepository.findByUserAndName(userId, newName)
            .ifPresent(existing -> { throw new CategoryGroupAlreadyExistsException(existing); });

        CategoryGroup group = CategoryGroup.builder()
            .userId(userId)
            .name(newName)
            .description(Strings.trimOrNull(description))
            .build();
        return categoryGroupRepository.save(group);
    }

    public CategoryGroup updateCategoryGroup(UUID userId, UUID groupId,
                                             String name, String description) {
        log.info("Updating category group [userId: {}, groupId: {}, name: {}]", userId, groupId, name);
        String newName = Strings.trimOrNull(name);
        if (newName == null) {
            throw new MissingParameterException("CategoryGroup.name");
        }

        CategoryGroup group = getCategoryGroup(userId, groupId);

        categoryGroupRepository.findByUserAndName(userId, newName)
            .filter(existing -> !existing.getId().equals(groupId))
            .ifPresent(existing -> { throw new CategoryGroupAlreadyExistsException(existing); });

        group.setName(newName);
        group.setDescription(Strings.trimOrNull(description));
        return categoryGroupRepository.save(group);
    }

    public CategoryGroup deleteCategoryGroup(UUID userId, UUID groupId) {
        log.info("Deleting category group [userId: {}, groupId: {}]", userId, groupId);
        CategoryGroup group = getCategoryGroup(userId, groupId);

        categoryGroupRepository.delete(group);
        return group;
    }

    /**
     * Deletes all category groups (and their categories and selectors) for the specified user.
     *
     * @param userId The user ID.
     */
    public void deleteAllCategoryGroups(UUID userId) {
        log.info("Deleting all category groups for user [userId: {}]", userId);
        categoryGroupRepository.deleteByUserId(userId);
    }

    /**
     * Returns the selected page of categories for the identified category group; in name order.
     *
     * @param groupId The category group ID.
     * @param pageIndex The zero-based, page number.
     * @param pageSize The max number of groups per page.
     * @return The selected page of category groups.
     */
    public Page<Category> getCategories(UUID userId, UUID groupId, int pageIndex, int pageSize) {
        log.info("Listing categories for group [userId: {}, groupId: {}, page: {}, pageSize: {}]",
            userId, groupId, pageIndex, pageSize);
        getCategoryGroup(userId, groupId);
        Page<Category> result = categoryRepository.findByGroupId(groupId, pageIndex, pageSize);

        if (log.isDebugEnabled()) {
            log.debug("Listing categories for group [groupId: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
                groupId, pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        }
        return result;
    }

    /**
     * Returns the identified category. Ensures the category belongs to the specified user.
     * @param userId The user ID.
     * @param categoryId The category ID.
     * @return The identified category.
     */
    public Category getCategory(UUID userId, UUID categoryId) {
        log.info("Getting category [userId: {}, categoryId: {}]", userId, categoryId);
        return validate(userId, categoryId);
    }

    public Category createCategory(UUID userId, UUID groupId, String name, String description, String colour) {
        log.info("Creating category [userId: {}, groupId: {}, name: {}]",
            userId, groupId, name);

        CategoryGroup group = getCategoryGroup(userId, groupId);

        Category category = group.addCategory(name, cat -> cat
            .description(Strings.trimOrNull(description))
            .colour(Strings.trimOrNull(colour))
        );

        categoryRepository.save(category);
        return category;
    }

    public Category updateCategory(UUID userId, UUID categoryId,
                                   String name, String description, String colour) {
        log.info("Updating category [userId: {}, categoryId: {}, name: {}]",
            userId, categoryId, name);

        String newName = Strings.trimOrNull(name);
        if (newName == null) {
            throw new MissingParameterException("Category.name");
        }

        Category category = validate(userId, categoryId);

        CategoryGroup group = category.getGroup();
        group.getCategory(newName)
            .filter(c -> !c.getId().equals(categoryId))
            .ifPresent(c -> { throw new CategoryAlreadyExistsException(c); });

        category.setName(newName);
        category.setDescription(Strings.trimOrNull(description));
        category.setColour(Strings.trimOrNull(colour));

        categoryRepository.save(category);
        return category;
    }

    /**
     * Deletes the identified category, if it belongs to the identified user.
     * All the selectors of the category will also be deleted.
     * @param userId the user attempting to delete the category.
     * @param categoryId the category's identifier.
     * @return the deleted category.
     */
    public Category deleteCategory(UUID userId, UUID categoryId) {
        log.info("Deleting category [userId: {}, categoryId: {}]",
            userId, categoryId);

        Category category = validate(userId, categoryId);
        categoryRepository.delete(category);
        return category;
    }

    public Page<CategorySelector> getCategorySelectors(UUID userId, UUID categoryId,
                                                       int pageIndex, int pageSize) {
        log.info("Listing category selectors [userId: {}, categoryId: {},, page: {}, pageSize: {}]",
            userId, categoryId, pageIndex, pageSize);

        Category category = validate(userId, categoryId);
        Page<CategorySelector> result = Page.of(category.getSelectors(), pageIndex, pageSize);

        if (log.isDebugEnabled()) {
            log.debug("Listing category selectors [userId: {}, categoryId: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
                userId, categoryId, pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        }
        return result;

    }
    /**
     * Returns the selectors for the identified category and account.
     *
     * @param userId The user ID to whom the category group and account must belong.
     * @param categoryId The category ID.
     * @param accountId The account ID to which the selectors are associated.
     * @return The category selectors.
     */
    public Page<CategorySelector> getCategorySelectors(UUID userId, UUID categoryId, UUID accountId,
                                                       int pageIndex, int pageSize) {
        log.info("Listing category selectors [userId: {}, categoryId: {}, accountId: {}, page: {}, pageSize: {},]",
            userId, categoryId, accountId, pageIndex, pageSize);

        Category category = validate(userId, categoryId);
        validateAccount(category.getGroup(), accountId);

        Page<CategorySelector> result = Page.of(category.getSelectors().stream()
            .filter(selector -> selector.getAccountId().equals(accountId))
            .toList(), pageIndex, pageSize);

        if (log.isDebugEnabled()) {
            log.debug("Listing category selectors [userId: {}, categoryId: {}, accountId: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
                userId, categoryId, accountId, pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        }
        return result;
    }

    /**
     * Sets the selectors for the identified category and account. Overwrites any existing selectors
     * for the same account.
     *
     * @param userId The user ID to whom the category and account must belong.
     * @param categoryId The category ID.
     * @param accountId The account ID to which the selectors are to be associated.
     * @param selectors The category selectors to be set.
     * @return the updated category selectors.
     */
    public Collection<CategorySelector> setCategorySelectors(UUID userId,
                                                             UUID categoryId,
                                                             UUID accountId,
                                                             Collection<CategorySelector> selectors) {
        log.info("Setting category selectors [userId: {}, categoryId: {}, accountId: {}]",
            userId, categoryId, accountId);

        Category category = validate(userId, categoryId);
        validateAccount(category.getGroup(), accountId);

        category.getSelectors().removeIf(selector -> selector.getAccountId().equals(accountId));
        if (selectors != null) {
            selectors.forEach(newSelector -> category.addSelector(accountId, selector -> selector
                .infoContains(Strings.trimOrNull(newSelector.getInfoContains()))
                .refContains(Strings.trimOrNull(newSelector.getRefContains()))
                .creditorContains(Strings.trimOrNull(newSelector.getCreditorContains()))
            ));
        }

        categoryRepository.save(category);
        return category.getSelectors();
    }

    /**
     * Generates the category statistics for the specified user, group and date range.
     * @param userId The user ID.
     * @param groupId The group ID.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (exclusive).
     * @return The list of category statistics.
     */
    public List<CategoryStatistics> getStatistics(UUID userId, UUID groupId, Instant startDate, Instant endDate) {
        CategoryGroup group = getCategoryGroup(userId, groupId);
        return categoryGroupRepository.getStatistics(group, startDate, endDate);
    }

    /**
     * Ensures that both the category group and account exist and belong to the specified user.
     * @param userId The user ID.
     * @param accountId The account ID.
     * @return The identified category group.
     * @throws NotFoundException if the group or account does not exist or does not belong to the user.
     */
    private CategoryGroup validate(UUID userId, UUID groupId, UUID accountId) {
        CategoryGroup group = getCategoryGroup(userId, groupId);
        validateAccount(group, accountId);
        return group;
    }

    /**
     * Ensures that the category exists and belongs to the specified user.
     * @param userId The user ID.
     * @param categoryId The category ID.
     * @return The identified category.
     * @throws NotFoundException if the category does not exist or does not belong to the user.
     */
    private Category validate(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdOptional(categoryId)
            .orElseThrow(() -> new NotFoundException("Category", categoryId));

        CategoryGroup group = category.getGroup();
        if (!group.getUserId().equals(userId)) {
            throw new NotFoundException("Category", categoryId);
        }

        return category;
    }

    /**
     * Ensures that the identified account exists and belongs to the same user as the given group.
     * @param group The category group.
     * @param accountId The account ID.
     * @throws NotFoundException if the account does not exist or does not belong to the group's user.
     */
    private void validateAccount(CategoryGroup group, UUID accountId) {
        accountRepository.findByIdOptional(accountId)
            .filter(account -> account.getUserId().equals(group.getUserId()))
            .orElseThrow(() -> new NotFoundException("Account", accountId));
    }
}
