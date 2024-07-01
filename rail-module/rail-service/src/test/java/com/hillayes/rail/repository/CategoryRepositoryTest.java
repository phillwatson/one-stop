package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class CategoryRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    CategoryRepository fixture;

    @Test
    public void testSaveSingleParent() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // when: persisting the category
        Category category = fixture.save(Category.builder()
            .userId(consent.getUserId())
            .name("category one")
            .description("Mock category one")
            .colour("#FF0000")
            .build());

        fixture.flush();

        // then: the category is persisted
        assertNotNull(category.getId());
    }

    @Test
    public void testSaveParentChild() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // when: persisting the category
        Category category = Category.builder()
            .userId(consent.getUserId())
            .name("category one")
            .description("Mock category one")
            .colour("#FF0000")
            .build();

        Category subcategory = category.addSubcategory(Category.builder()
            .userId(consent.getUserId())
            .name("subcategory one/one")
            .description("Mock subcategory one/one")
            .colour("#FF00FF")
            .build());

        fixture.save(category);
        fixture.flush();

        // then: the category is persisted
        assertNotNull(category.getId());

        // and: the subcategory is persisted
        assertNotNull(subcategory.getId());

        // and: the subcategory is linked to the parent
        assertEquals(category, subcategory.getParent());
    }

    @Test
    public void testRemoveSubcategory() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a category with subcategories
        Category category = Category.builder()
            .userId(consent.getUserId())
            .name("category one")
            .description("Mock category one")
            .colour("#FF0000")
            .build();

        Category subcategory1 = category.addSubcategory(Category.builder()
            .userId(consent.getUserId())
            .name("subcategory one/one")
            .description("Mock subcategory one/one")
            .colour("#FF00FF")
            .build());

        Category subcategory2 = category.addSubcategory(Category.builder()
            .userId(consent.getUserId())
            .name("subcategory one/two")
            .description("Mock subcategory one/two")
            .colour("#FF0066")
            .build());

        // when: the parent category is saved
        fixture.save(category);
        fixture.flush();

        // then: the subcategories are saved
        assertTrue(fixture.findByIdOptional(subcategory2.getId()).isPresent());
        assertTrue(fixture.findByIdOptional(subcategory1.getId()).isPresent());

        // when: removing a subcategory
        assertTrue(category.removeSubcategory(subcategory2));

        // then: the subcategory is removed
        assertFalse(category.getSubcategories().contains(subcategory2));

        // and: the subcategory is no longer linked to the parent
        assertNull(subcategory2.getParent());

        // when: the parent is persisted
        fixture.saveAndFlush(category);

        // then: the removed subcategory no longer exists
        assertTrue(fixture.findByIdOptional(subcategory2.getId()).isEmpty());

        // and: the remaining subcategory still exists
        assertTrue(fixture.findByIdOptional(subcategory1.getId()).isPresent());
    }

    @Test
    public void testSelectors() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category
        Category category = Category.builder()
            .userId(consent.getUserId())
            .name("category one")
            .description("Mock category one")
            .colour("#FF0000")
            .build();

        // when: a selector is added
        CategorySelector selector = category.addSelector(account, ".*");

        // then: the selector is linked to the category
        assertEquals(category, selector.getCategory());

        // and: the selector is linked to the account
        assertEquals(account, selector.getAccount());

        // when: the category is persisted
        fixture.save(category);
        fixture.flush();

        // then: the selector is persisted
        assertNotNull(selector.getId());
    }

    @Test
    public void testDeleteParent() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category
        Category category = Category.builder()
            .userId(consent.getUserId())
            .name("category one")
            .description("Mock category one")
            .colour("#FF0000")
            .build();

        // and: the category has a subcategory
        Category subcategory1 = category.addSubcategory(Category.builder()
            .userId(consent.getUserId())
            .name("subcategory one/one")
            .description("Mock subcategory one/one")
            .colour("#FF00FF")
            .build());

        // and: the category has another subcategory
        Category subcategory2 = category.addSubcategory(Category.builder()
            .userId(consent.getUserId())
            .name("subcategory one/two")
            .description("Mock subcategory one/two")
            .colour("#FF0066")
            .build());

        // when: a selector is added to each category
        List<CategorySelector> selectors = List.of(
            category.addSelector(account, ".*"),
            subcategory1.addSelector(account, ".*"),
            subcategory2.addSelector(account, ".*")
        );

        // when: the parent category is saved
        fixture.save(category);
        fixture.flush();

        // then: all sub-categories are saved
        assertNotNull(subcategory1.getId());
        assertNotNull(subcategory2.getId());

        // and: all selectors are saved
        selectors.forEach(selector -> assertNotNull(selector.getId()));

        // when: the parent category is deleted
        fixture.delete(category);
        fixture.flush();

        // then: the parent category is deleted
        assertTrue(fixture.findByIdOptional(category.getId()).isEmpty());

        // and: all sub-categories are deleted
        assertTrue(fixture.findByIdOptional(subcategory1.getId()).isEmpty());

        // and: all selectors are deleted
        selectors.forEach(selector -> assertTrue(fixture.findByIdOptional(selector.getId()).isEmpty()));
    }


    private UserConsent mockUserConsent() {
        return TestData.mockUserConsent(UUID.randomUUID(), consent -> {
            consent.id(null);
        });
    }

    private Account mockAccount(UserConsent consent) {
        return TestData.mockAccount(consent.getUserId(), account -> {
            account.id(null);
            account.userConsentId(consent.getId());
            account.institutionId(consent.getInstitutionId());
        });
    }
}
