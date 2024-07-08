package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.*;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class CategoryServiceTest {
    @Mock
    CategoryRepository categoryRepository;

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    CategoryService fixture;

    @BeforeEach
    public void clearCaches() {
        openMocks(this);
    }

    @Test
    public void testGetCategories() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the repository returns a result
        when(categoryRepository.findByUserId(eq(userId), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // when: the categories are requested
        fixture.getCategories(userId, 0, 20);

        // then: the categories are retrieved from the repository
        verify(categoryRepository).findByUserId(userId, 0, 20);
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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // and: the category has selectors associated with the account
        List<CategorySelector> expectedSelectors = List.of(
            category.addSelector(account.getId(), selector -> selector.infoContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.refContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
        );

        // and: the category has selectors associated with other accounts
        category.addSelector(UUID.randomUUID(), selector -> selector.infoContains(randomAlphanumeric(10)));
        category.addSelector(UUID.randomUUID(), selector -> selector.refContains(randomAlphanumeric(10)));
        category.addSelector(UUID.randomUUID(), selector -> selector.creditorContains(randomAlphanumeric(10)));

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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

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
        when(categoryRepository.findByIdOptional(categoryId))
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
    public void testGetCategorySelectors_InvalidCategoryId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category NOT belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // and: the category has selectors associated with the account
        List<CategorySelector> oldSelectors = List.of(
            category.addSelector(account.getId(), selector -> selector.infoContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.refContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
        );

        // and: the new selectors to replace the old ones
        List<CategorySelector> newSelectors = List.of(
            CategorySelector.builder().infoContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().refContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().creditorContains(randomAlphanumeric(10)).build()
        );

        // when: the category selectors are set
        fixture.setCategorySelectors(userId, category.getId(), account.getId(), newSelectors);

        // then: the category is retrieved from the repository
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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // and: the category has selectors associated with the account
        List<CategorySelector> oldSelectors = List.of(
            category.addSelector(account.getId(), selector -> selector.infoContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.refContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10))),
            category.addSelector(account.getId(), selector -> selector.creditorContains(randomAlphanumeric(10)))
        );

        // when: the category selectors are set to an empty collection
        fixture.setCategorySelectors(userId, category.getId(), account.getId(), newSelectors);

        // then: the category is retrieved from the repository
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

        // and: a category belonging to that user - with NO selectors
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // and: the new selectors to replace the old ones
        List<CategorySelector> newSelectors = List.of(
            CategorySelector.builder().infoContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().refContains(randomAlphanumeric(10)).build(),
            CategorySelector.builder().creditorContains(randomAlphanumeric(10)).build()
        );

        // when: the category selectors are set
        fixture.setCategorySelectors(userId, category.getId(), account.getId(), newSelectors);

        // then: the category is retrieved from the repository
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
        when(categoryRepository.findByIdOptional(categoryId))
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
        verify(categoryRepository, never()).save(any());
    }

    @Test
    public void testSetCategorySelectors_InvalidCategoryId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an account belonging to that user
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: a category NOT belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, category.getId(), account.getId(), List.of())
        );

        // and: the exception identifies the category
        assertEquals("Category", exception.getParameter("entity-type"));
        assertEquals(category.getId(), exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryRepository, never()).save(any());
    }

    @Test
    public void testSetCategorySelectors_UnknownAccountId() {
        // given: a user id, a category id, and an account id
        UUID userId = UUID.randomUUID();

        // and: an unknown account id
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdOptional(accountId))
            .thenReturn(Optional.empty());

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, category.getId(), accountId, List.of())
        );

        // and: the exception identifies the account
        assertEquals("Account", exception.getParameter("entity-type"));
        assertEquals(accountId, exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryRepository, never()).save(any());
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

        // and: a category belonging to that user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .build();
        when(categoryRepository.findByIdOptional(category.getId()))
            .thenReturn(Optional.of(category));

        // when: the category selectors are set
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            // then: an exception is thrown
            fixture.setCategorySelectors(userId, category.getId(), account.getId(), List.of())
        );

        // and: the exception identifies the account
        assertEquals("Account", exception.getParameter("entity-type"));
        assertEquals(account.getId(), exception.getParameter("entity-id"));

        // and: no updates are made to the category
        verify(categoryRepository, never()).save(any());
    }

    @Test
    public void testDeleteAllCategories() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // when: all categories are deleted
        fixture.deleteAllCategories(userId);

        // then: the categories are deleted from the repository
        verify(categoryRepository).deleteByUserId(userId);
    }
}
