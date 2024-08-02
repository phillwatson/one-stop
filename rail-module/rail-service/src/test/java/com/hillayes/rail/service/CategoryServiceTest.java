package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.errors.CategoryAlreadyExistsException;
import com.hillayes.rail.errors.CategoryGroupAlreadyExistsException;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.CategoryGroupRepository;
import com.hillayes.rail.repository.CategoryRepository;
import com.hillayes.rail.utils.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class CategoryServiceTest {
    @Mock
    CategoryGroupRepository categoryGroupRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    CategoryService fixture;

    @BeforeEach
    public void initTests() {
        openMocks(this);

        // mock the save method to return a new UUID
        when(categoryGroupRepository.save(any())).then(invocation -> {
            CategoryGroup result = invocation.getArgument(0);
            if (result.getId() == null)
                result.setId(UUID.randomUUID());
            return result;
        });
    }

    @Test
    public void testGetCategortGroups() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the repository returns a result
        when(categoryGroupRepository.findByUserId(eq(userId), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // when: the categories are requested
        fixture.getCategoryGroups(userId, 0, 20);

        // then: the categories are retrieved from the repository
        verify(categoryGroupRepository).findByUserId(userId, 0, 20);
    }

    @Test
    public void testGetCategoryGroup() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // when: the category group is requested
        CategoryGroup actualGroup = fixture.getCategoryGroup(userId, group.getId());

        // then: the category group is retrieved from the repository
        verify(categoryGroupRepository).findByIdOptional(group.getId());

        // and: the category group is returned
        assertEquals(group.getId(), actualGroup.getId());
    }

    @Test
    public void testCreateCategoryGroup() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a new category group details
        String name = randomAlphanumeric(20);
        String description = randomAlphanumeric(20);

        // and: no existing category with the same name
        when(categoryGroupRepository.findByUserAndName(userId, name))
            .thenReturn(Optional.empty());

        // when: the category group is created
        CategoryGroup group = fixture.createCategoryGroup(userId, name, description);

        // then: the category is retrieved from the repository
        verify(categoryGroupRepository).findByUserAndName(userId, name);

        // then: the category is saved to the repository
        ArgumentCaptor<CategoryGroup> groupCaptor = ArgumentCaptor.forClass(CategoryGroup.class);
        verify(categoryGroupRepository).save(groupCaptor.capture());

        // and: the category is returned
        assertEquals(groupCaptor.getValue(), group);
    }

    @Test
    public void testCreateCategoryGroup_AlreadyExists() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a new category group details
        String name = randomAlphanumeric(20);
        String description = randomAlphanumeric(20);

        // and: a group with the same name already exists
        CategoryGroup existingCategory = mockCategoryGroup(userId, name);

        // when: the category group is created
        CategoryGroupAlreadyExistsException exception = assertThrows(CategoryGroupAlreadyExistsException.class, () ->
            // then: an exception is thrown
            fixture.createCategoryGroup(userId, name, description)
        );

        // and: the exception identifies the category
        assertEquals(existingCategory.getId(), exception.getParameter("id"));
        assertEquals(existingCategory.getName(), exception.getParameter("name"));

        // and: the category group is not created
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testUpdateCategoryGroup() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);

        // and: no group with the same name
        when(categoryGroupRepository.findByUserAndName(userId, name))
            .thenReturn(Optional.empty());

        // when: the category group is updated
        CategoryGroup result = fixture.updateCategoryGroup(userId, group.getId(), name, description);

        // then: the category group is updated
        ArgumentCaptor<CategoryGroup> groupCaptor = ArgumentCaptor.forClass(CategoryGroup.class);
        verify(categoryGroupRepository).save(groupCaptor.capture());
        CategoryGroup updatedGroup = groupCaptor.getValue();

        // and: the updated category group is as requested
        assertEquals(group.getId(), updatedGroup.getId());
        assertEquals(name, updatedGroup.getName());
        assertEquals(description, updatedGroup.getDescription());

        // and: the updated category group is returned
        assertEquals(updatedGroup, result);
    }

    @Test
    public void testUpdateCategoryGroup_InvalidGroupId() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an invalid category group ID
        UUID groupId = UUID.randomUUID();
        when(categoryGroupRepository.findByIdOptional(groupId))
            .thenReturn(Optional.empty());

        // and: a new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);

        // and: no group with the same name
        when(categoryGroupRepository.findByUserAndName(userId, name))
            .thenReturn(Optional.empty());

        // when: the category group is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.updateCategoryGroup(userId, groupId, name, description)
        );

        // and: the exception identifies the category
        assertEquals("CategoryGroup", exception.getParameter("entity-type"));
        assertEquals(groupId, exception.getParameter("entity-id"));
    }

    @Test
    public void testUpdateCategoryGroup_AlreadyExists() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);

        // and: a group with the same name already exists
        CategoryGroup existingCategory = mockCategoryGroup(userId, name);

        // when: the category group is updated
        CategoryGroupAlreadyExistsException exception = assertThrows(CategoryGroupAlreadyExistsException.class, () ->
            // then: an exception is thrown
            fixture.updateCategoryGroup(userId, group.getId(), name, description)
        );

        // and: the exception identifies the category
        assertEquals(existingCategory.getId(), exception.getParameter("id"));
        assertEquals(existingCategory.getName(), exception.getParameter("name"));

        // and: the category group is not created
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testUpdateCategoryGroup_WrongUser() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // and: a new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);

        // when: the category group is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.updateCategoryGroup(userId, group.getId(), name, description)
        );

        // and: the exception identifies the category
        assertEquals("CategoryGroup", exception.getParameter("entity-type"));
        assertEquals(group.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteCategoryGroup() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // when: the category group is deleted
        CategoryGroup deletedGroup = fixture.deleteCategoryGroup(userId, group.getId());

        // then: the category group is deleted from the repository
        ArgumentCaptor<CategoryGroup> groupCaptor = ArgumentCaptor.forClass(CategoryGroup.class);
        verify(categoryGroupRepository).delete(groupCaptor.capture());
        assertEquals(group, groupCaptor.getValue());

        // and: the deleted group is returned
        assertEquals(group, deletedGroup);
    }

    @Test
    public void testDeleteCategoryGroup_InvalidGroupId() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an invalid category group ID
        UUID groupId = UUID.randomUUID();
        when(categoryGroupRepository.findByIdOptional(groupId))
            .thenReturn(Optional.empty());

        // when: the category group is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.deleteCategoryGroup(userId, groupId)
        );

        // and: the exception identifies the category group
        assertEquals("CategoryGroup", exception.getParameter("entity-type"));
        assertEquals(groupId, exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteCategoryGroup_WrongUser() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // when: the category group is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.deleteCategoryGroup(userId, group.getId())
        );

        // and: the exception identifies the category group
        assertEquals("CategoryGroup", exception.getParameter("entity-type"));
        assertEquals(group.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteAllCategoryGroups() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // when: all categories are deleted
        fixture.deleteAllCategoryGroups(userId);

        // then: the categories are deleted from the repository
        verify(categoryGroupRepository).deleteByUserId(userId);
    }

    @Test
    public void testGetCategories() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: the repository returns a result
        when(categoryRepository.findByGroupId(eq(group.getId()), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // when: the categories are requested
        fixture.getCategories(userId, group.getId(), 0, 20);

        // then: the categories are retrieved from the repository
        verify(categoryRepository).findByGroupId(group.getId(), 0, 20);
    }

    @Test
    public void testGetCategory() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: the group has a category
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category is requested
        Category actualCategory = fixture.getCategory(userId, category.getId());

        // then: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(category.getId());

        // and: the category is returned
        assertEquals(category.getId(), actualCategory.getId());
    }

    @Test
    public void testGetCategory_WrongUser() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category is requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.getCategory(userId, category.getId())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(category.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testGetCategory_InvalidCategoryId() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an unknown category id
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdOptional(categoryId))
            .thenReturn(Optional.empty());

        // when: the category is requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.getCategory(userId, categoryId)
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(categoryId, exception.getParameter("entity-id"));
    }

    @Test
    public void testCreateCategory() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);
        String colour = randomAlphanumeric(10);

        // when: the category is created
        Category result = fixture.createCategory(userId, group.getId(), name, description, colour);

        // then: the category is saved to the repository
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());

        // and: the category is returned
        assertEquals(categoryCaptor.getValue(), result);
    }

    @Test
    public void testCreateCategory_AlreadyExists() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: that group has a named category
        Category existingCategory = group.addCategory(randomAlphanumeric(20), b -> b.id(UUID.randomUUID()));

        // and: a new category details with the same name
        String name = existingCategory.getName();
        String description = randomAlphanumeric(10);
        String colour = randomAlphanumeric(10);

        // when: the category is created
        CategoryAlreadyExistsException exception = assertThrows(CategoryAlreadyExistsException.class, () ->
            // then: an exception is thrown
            fixture.createCategory(userId, group.getId(), name, description, colour)
        );

        // and: the exception identifies the category
        assertEquals(existingCategory.getId(), exception.getParameter("id"));
        assertEquals(existingCategory.getName(), exception.getParameter("name"));

        // and: the category is not created
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testUpdateCategory() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: that group has a named category
        Category existingCategory = mockCategory(group, randomAlphanumeric(20));

        // and: the new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);
        String colour = randomAlphanumeric(10);

        // when: the category is updated
        Category result = fixture.updateCategory(userId, existingCategory.getId(), name, description, colour);

        // then: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(existingCategory.getId());

        // and: the category is saved to the repository
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());

        // and: the updated category is as requested
        Category updatedCategory = categoryCaptor.getValue();
        assertEquals(existingCategory.getId(), updatedCategory.getId());
        assertEquals(name, updatedCategory.getName());
        assertEquals(description, updatedCategory.getDescription());
        assertEquals(colour, updatedCategory.getColour());

        // and: the response matches the updated category
        assertEquals(existingCategory.getId(), result.getId());
        assertEquals(name, result.getName());
        assertEquals(description, result.getDescription());
        assertEquals(colour, result.getColour());
    }

    @Test
    public void testUpdateCategory_WrongUser() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // and: the new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);
        String colour = randomAlphanumeric(10);

        // when: the category is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.updateCategory(userId, category.getId(), name, description, colour)
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(category.getId(), exception.getParameter("entity-id"));

        // and: the category is not updated
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testUpdateCategory_InvalidCategoryId() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an unknown category id
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdOptional(categoryId))
            .thenReturn(Optional.empty());

        // and: the new category details
        String name = randomAlphanumeric(10);
        String description = randomAlphanumeric(10);
        String colour = randomAlphanumeric(10);

        // when: the category is updated
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.updateCategory(userId, categoryId, name, description, colour)
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(categoryId, exception.getParameter("entity-id"));

        // and: the category is not updated
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testGetCategorySelectors() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // and: the category has selectors associated with the account
        Set<CategorySelector> expectedSelectors = new HashSet(category
            .addSelector(account.getId(), selector -> selector.infoContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.refContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
            .getSelectors());

        // and: the category has selectors associated with other accounts
        category
            .addSelector(UUID.randomUUID(), selector -> selector.infoContains(randomAlphanumeric(10)))
            .addSelector(UUID.randomUUID(), selector -> selector.refContains(randomAlphanumeric(10)))
            .addSelector(UUID.randomUUID(), selector -> selector.creditorContains(randomAlphanumeric(10)));

        // when: the category selectors are requested
        Collection<CategorySelector> actualSelectors =
            fixture.getCategorySelectors(userId, category.getId(), account.getId());

        // then: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(category.getId());

        // and: only the identified account's selectors are returned
        assertEquals(expectedSelectors.size(), actualSelectors.size());
        expectedSelectors.forEach(expected ->
            assertTrue(actualSelectors.contains(expected))
        );
    }

    @Test
    public void testDeleteCategory() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category is deleted
        Category deletedCategory = fixture.deleteCategory(userId, category.getId());

        // then: the category is deleted from the repository
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).delete(categoryCaptor.capture());
        assertEquals(category, categoryCaptor.getValue());

        // and: the deleted category is returned
        assertEquals(category, deletedCategory);
    }

    @Test
    public void testDeleteCategory_InvalidCategoryId() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an invalid category ID
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdOptional(categoryId))
            .thenReturn(Optional.empty());

        // when: the category is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.deleteCategory(userId, categoryId)
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(categoryId, exception.getParameter("entity-id"));
    }

    @Test
    public void testDeleteCategory_WrongUser() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category is deleted
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.deleteCategory(userId, category.getId())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(category.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testGetCategorySelectors_NonFound() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // and: the category has selectors associated with other accounts
        category.addSelector(UUID.randomUUID(), selector -> selector.infoContains(randomAlphanumeric(10)));
        category.addSelector(UUID.randomUUID(), selector -> selector.refContains(randomAlphanumeric(10)));
        category.addSelector(UUID.randomUUID(), selector -> selector.creditorContains(randomAlphanumeric(10)));

        // when: the category selectors are requested
        Collection<CategorySelector> actualSelectors =
            fixture.getCategorySelectors(userId, category.getId(), account.getId());

        // then: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(category.getId());

        // and: NO selectors are returned
        assertTrue(actualSelectors.isEmpty());
    }

    @Test
    public void testGetCategorySelectors_UnknownCategoryId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: an unknown category id
        UUID categoryId = UUID.randomUUID();
        when(categoryGroupRepository.findByIdOptional(categoryId))
            .thenReturn(Optional.empty());

        // when: the category selectors are requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.getCategorySelectors(userId, categoryId, account.getId())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(categoryId, exception.getParameter("entity-id"));
    }

    @Test
    public void testGetCategorySelectors_WrongUser() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category selectors are requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.getCategorySelectors(userId, category.getId(), account.getId())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(category.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testGetCategorySelectors_UnknownAccountId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an unknown account id
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdOptional(accountId))
            .thenReturn(Optional.empty());

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category selectors are requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.getCategorySelectors(userId, category.getId(), accountId)
        );

        // and: the exception identifies the account
        assertEquals("Account", exception.getParameter("entity-type"));
        assertEquals(accountId, exception.getParameter("entity-id"));
    }

    @Test
    public void testGetCategorySelectors_InvalidAccountId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account NOT belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category selectors are requested
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.getCategorySelectors(userId, category.getId(), account.getId())
        );

        // and: the exception identifies the account
        assertEquals("Account", exception.getParameter("entity-type"));
        assertEquals(account.getId(), exception.getParameter("entity-id"));
    }

    @Test
    public void testSetCategorySelectors() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // and: the category has selectors associated with the account
        Set<CategorySelector> oldSelectors = new HashSet(category
            .addSelector(account.getId(), selector -> selector.infoContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.refContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
            .getSelectors());

        // and: the new selectors to replace the old ones
        List<CategorySelector> newSelectors = List.of(
            CategorySelector.builder().infoContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().refContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().creditorContains(randomAlphanumeric(10)).build()
        );

        // when: the category selectors are set
        Collection<CategorySelector> updatedSelectors = fixture.setCategorySelectors(userId, category.getId(), account.getId(), newSelectors);

        // then: the response contains the updated selectors
        assertEquals(newSelectors.size(), updatedSelectors.size());
        newSelectors.forEach(newSelector ->
            assertTrue(updatedSelectors.stream()
                .anyMatch(s -> Objects.equals(s.getInfoContains(), newSelector.getInfoContains())
                    || Objects.equals(s.getRefContains(), newSelector.getRefContains())
                    || Objects.equals(s.getCreditorContains(), newSelector.getCreditorContains())))
        );

        // and: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(category.getId());

        // and: the updated category is saved
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category updatedCategory = categoryCaptor.getValue();

        // and: the old selectors are removed
        oldSelectors.forEach(oldSelector ->
            assertFalse(updatedCategory.getSelectors().contains(oldSelector)));

        // and: the category's selectors are updated
        assertEquals(newSelectors.size(), updatedCategory.getSelectors().size());
        newSelectors.forEach(newSelector ->
            assertTrue(category.getSelectors().stream()
                .anyMatch(s -> Objects.equals(s.getInfoContains(), newSelector.getInfoContains())
                    || Objects.equals(s.getRefContains(), newSelector.getRefContains())
                    || Objects.equals(s.getCreditorContains(), newSelector.getCreditorContains())))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testSetCategorySelectors_EmptyNewSelectors(List<CategorySelector> newSelectors) {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // and: the category has selectors associated with the account
        Set<CategorySelector> oldSelectors = new HashSet(category
            .addSelector(account.getId(), selector -> selector.infoContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.refContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
            .addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
            .getSelectors());

        // when: the category selectors are set to an empty collection
        Collection<CategorySelector> updatedSelectors = fixture.setCategorySelectors(userId, category.getId(), account.getId(), newSelectors);

        // then: the response contains the updated selectors
        if (newSelectors != null) {
            assertEquals(newSelectors.size(), updatedSelectors.size());
            newSelectors.forEach(newSelector ->
                assertTrue(updatedSelectors.stream()
                    .anyMatch(s -> Objects.equals(s.getInfoContains(), newSelector.getInfoContains())
                        || Objects.equals(s.getRefContains(), newSelector.getRefContains())
                        || Objects.equals(s.getCreditorContains(), newSelector.getCreditorContains())))
            );
        } else {
            assertTrue(updatedSelectors.isEmpty());
        }

        // and: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(category.getId());

        // and: the updated category is saved
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category updatedCategory = categoryCaptor.getValue();

        // and: the old selectors are removed
        oldSelectors.forEach(oldSelector ->
            assertFalse(updatedCategory.getSelectors().contains(oldSelector)));

        // and: the category's selectors are cleared
        assertTrue(updatedCategory.getSelectors().isEmpty());
    }

    @Test
    public void testSetCategorySelectors_NoOldSelectors() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group - with NO selectors
        Category category = mockCategory(group, randomAlphanumeric(20));

        // and: the new selectors to replace the old ones
        List<CategorySelector> newSelectors = List.of(
            CategorySelector.builder().infoContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().refContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().creditorContains(randomAlphanumeric(10)).build()
        );

        // when: the category selectors are set
        Collection<CategorySelector> updatedSelectors = fixture.setCategorySelectors(userId, category.getId(), account.getId(), newSelectors);

        // then: the response contains the updated selectors
        assertEquals(newSelectors.size(), updatedSelectors.size());
        newSelectors.forEach(newSelector ->
            assertTrue(updatedSelectors.stream()
                .anyMatch(s -> Objects.equals(s.getInfoContains(), newSelector.getInfoContains())
                    || Objects.equals(s.getRefContains(), newSelector.getRefContains())
                    || Objects.equals(s.getCreditorContains(), newSelector.getCreditorContains())))
        );

        // and: the category is retrieved from the repository
        verify(categoryRepository).findByIdOptional(category.getId());

        // and: the updated category is saved
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category updatedCategory = categoryCaptor.getValue();

        // and: the category's selectors are updated
        assertEquals(newSelectors.size(), updatedCategory.getSelectors().size());
        newSelectors.forEach(newSelector ->
            assertTrue(category.getSelectors().stream()
                .anyMatch(s -> Objects.equals(s.getInfoContains(), newSelector.getInfoContains())
                    || Objects.equals(s.getRefContains(), newSelector.getRefContains())
                    || Objects.equals(s.getCreditorContains(), newSelector.getCreditorContains())))
        );
    }

    @Test
    public void testSetCategorySelectors_UnknownCategoryId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: an unknown category id
        UUID categoryId = UUID.randomUUID();
        when(categoryGroupRepository.findByIdOptional(categoryId))
            .thenReturn(Optional.empty());

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, categoryId, account.getId(), List.of())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(categoryId, exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testSetCategorySelectors_WrongUser() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to another user
        CategoryGroup group = mockCategoryGroup(UUID.randomUUID(), randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, category.getId(), account.getId(), List.of())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(category.getId(), exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testSetCategorySelectors_UnknownAccountId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an unknown account id
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdOptional(accountId))
            .thenReturn(Optional.empty());

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, category.getId(), accountId, List.of())
        );

        // and: the exception identifies the account
        assertEquals("Account", exception.getParameter("entity-type"));
        assertEquals(accountId, exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testSetCategorySelectors_InvalidAccountId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account NOT belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a category belonging to that group
        Category category = mockCategory(group, randomAlphanumeric(20));

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, category.getId(), account.getId(), List.of())
        );

        // and: the exception identifies the account
        assertEquals("Account", exception.getParameter("entity-type"));
        assertEquals(account.getId(), exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryGroupRepository, never()).save(any());
    }

    @Test
    public void testGetStatistics() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a category group belonging to that user
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: a date range
        Instant startDate = Instant.now().minus(Duration.ofDays(7));
        Instant endDate = Instant.now();

        // and: the repository is primed with data
        List<CategoryStatistics> expected = List.of(
            TestData.mockCategoryStatistics(group, "cat-1", 20, 123.44, 282.93,11.25),
            TestData.mockCategoryStatistics(group, "cat-2", 10, 456.44, 222.73,21.225),
            TestData.mockCategoryStatistics(group, "cat-3", 6, 34.44, 82.73,177.25)
        );
        when(categoryGroupRepository.getStatistics(group, startDate, endDate))
            .thenReturn(expected);

        // when: the service is called to retrieve the statistics
        List<CategoryStatistics> result = fixture.getStatistics(userId, group.getId(), startDate, endDate);

        // then: the request is passed to the repository
        verify(categoryGroupRepository).getStatistics(group, startDate, endDate);

        // and: the result is as expected
        assertEquals(expected, result);
    }

    private CategoryGroup mockCategoryGroup(UUID userId, String name) {
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .name(name)
            .build();
        when(categoryGroupRepository.findByIdOptional(group.getId()))
            .thenReturn(Optional.of(group));
        when(categoryGroupRepository.findByUserAndName(userId, name))
            .thenReturn(Optional.of(group));

        return group;
    }

    private Category mockCategory(CategoryGroup group, String name) {
        // and: a category belonging to that group
        Category category = group.addCategory(name, b -> b.id(UUID.randomUUID()));
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));
        return category;
    }
}
