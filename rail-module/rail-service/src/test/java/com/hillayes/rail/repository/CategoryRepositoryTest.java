package com.hillayes.rail.repository;

import com.hillayes.rail.domain.*;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class CategoryRepositoryTest {
    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    CategoryGroupRepository categoryGroupRepository;

    @Inject
    CategoryRepository fixture;

    @Test
    public void testSave() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a category group for the user
        CategoryGroup categoryGroup = categoryGroupRepository.save(CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .build());

        // when: persisting the category
        Category category = fixture.save(Category.builder()
            .group(categoryGroup)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .colour("#FF0000")
            .build());

        fixture.flush();

        // then: the category is persisted
        assertNotNull(category.getId());
    }

    @Test
    public void testSaveSelectors() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category group for the user
        CategoryGroup categoryGroup = categoryGroupRepository.save(CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .build());

        // and: a category
        Category category = Category.builder()
            .group(categoryGroup)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .colour("#FF0000")
            .build();

        // when: selectors are added
        category.add(account.getId(), builder -> builder
            .infoContains(insecure().nextAlphanumeric(10))
            .refContains(insecure().nextAlphanumeric(20))
            .creditorContains(insecure().nextAlphanumeric(15))
        );
        category.add(account.getId(), builder ->
            builder.infoContains(insecure().nextAlphanumeric(12))
        );
        category.add(account.getId(), builder ->
            builder.refContains(insecure().nextAlphanumeric(25))
        );
        category.add(account.getId(), builder ->
            builder.creditorContains(insecure().nextAlphanumeric(10))
        );
        List<CategorySelector> selectors = category.getSelectors();

        // then: the selectors are linked to the category
        selectors.forEach(selector -> assertEquals(category, selector.getCategory()));

        // and: the selectors are linked to the account
        selectors.forEach(selector -> assertEquals(account.getId(), selector.getAccountId()));

        // when: the category is persisted
        fixture.save(category);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the selectors are persisted
        selectors.forEach(selector -> assertNotNull(selector.getId()));

        // when: the category is retrieved
        Category retrieved = fixture.findByIdOptional(category.getId()).orElseThrow();

        // then: the selectors are retrieved
        assertEquals(selectors.size(), retrieved.getSelectors().size());
    }

    @Test
    public void testMoveSelector() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category group for the user
        CategoryGroup categoryGroup = categoryGroupRepository.save(CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .build());

        // and: a category
        Category category = Category.builder()
            .group(categoryGroup)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .colour("#FF0000")
            .build();

        // and: selectors are added
        category.add(account.getId(), builder -> builder
            .infoContains(insecure().nextAlphanumeric(10))
            .refContains(insecure().nextAlphanumeric(20))
            .creditorContains(insecure().nextAlphanumeric(15))
        );
        category.add(account.getId(), builder ->
            builder.infoContains(insecure().nextAlphanumeric(12))
        );
        category.add(account.getId(), builder ->
            builder.refContains(insecure().nextAlphanumeric(25))
        );
        category.add(account.getId(), builder ->
            builder.creditorContains(insecure().nextAlphanumeric(10))
        );
        List<CategorySelector> selectors = category.getSelectors();

        // then: the selectors are linked to the category
        for (CategorySelector selector : selectors) {
            assertEquals(category, selector.getCategory());
        }

        // and: the selectors are linked to the account
        selectors.forEach(selector -> assertEquals(account.getId(), selector.getAccountId()));

        // and: the category is persisted
        fixture.saveAndFlush(category);
        assertEquals(4, category.getSelectors().size());

        // and: another category exists
        Category destCategory = fixture.saveAndFlush(Category.builder()
            .group(categoryGroup)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .colour("#FF0011")
            .build());
        assertEquals(0, destCategory.getSelectors().size());
        fixture.saveAndFlush(destCategory);

        // when: a selector is moved from the first category to the second
        CategorySelector selector = category.getSelectors().get(1);
        destCategory.add(selector);

        // then: the selector is moved from the original
        assertEquals(3, category.getSelectors().size());
        assertEquals(1, destCategory.getSelectors().size());
        assertEquals(selector, destCategory.getSelectors().get(0));

        // when: the two categories are saved
        fixture.save(category);
        fixture.save(destCategory);

        fixture.flush();
        fixture.getEntityManager().clear();

        // and: the categories are read again
        category = fixture.findByIdOptional(category.getId()).orElseThrow();
        destCategory = fixture.findByIdOptional(destCategory.getId()).orElseThrow();

        // then: the selector is moved from the original
        assertEquals(3, category.getSelectors().size());
        assertEquals(1, destCategory.getSelectors().size());
    }

    @Test
    public void testDeleteSelector() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category group for the user
        CategoryGroup categoryGroup = categoryGroupRepository.save(CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .build());

        // and: a category
        Category category = Category.builder()
            .group(categoryGroup)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .colour("#FF0000")
            .build();

        // and: selectors are added
        category.add(account.getId(), builder -> builder
            .infoContains(insecure().nextAlphanumeric(10))
            .refContains(insecure().nextAlphanumeric(20))
            .creditorContains(insecure().nextAlphanumeric(15))
        );
        category.add(account.getId(), builder ->
            builder.infoContains(insecure().nextAlphanumeric(12))
        );
        category.add(account.getId(), builder ->
            builder.refContains(insecure().nextAlphanumeric(25))
        );
        category.add(account.getId(), builder ->
            builder.creditorContains(insecure().nextAlphanumeric(10))
        );

        // and: the category is persisted
        fixture.saveAndFlush(category);
        assertEquals(4, category.getSelectors().size());

        // when: a selector is removed
        CategorySelector selector = category.getSelectors().get(1);
        category.remove(selector);

        // then: the selector is moved from the original
        assertEquals(3, category.getSelectors().size());

        // when: the category is saved
        fixture.saveAndFlush(category);
        fixture.getEntityManager().clear();

        // and: the category are read again
        category = fixture.findByIdOptional(category.getId()).orElseThrow();

        // then: the selector is removed
        assertEquals(3, category.getSelectors().size());
        assertNull(category.getSelectors().stream()
            .filter(s -> s.getId().equals(selector.getId()))
            .findFirst().orElse(null));
    }

    @Test
    public void testDeleteCategory() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category group for the user
        CategoryGroup categoryGroup = categoryGroupRepository.save(CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .build());

        // and: a category
        Category category = Category.builder()
            .group(categoryGroup)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .colour("#FF0000")
            .build();

        // when: selectors are added to the category
        category.add(account.getId(), builder -> builder.infoContains(insecure().nextAlphanumeric(20)));
        category.add(account.getId(), builder -> builder.refContains(insecure().nextAlphanumeric(5)));
        category.add(account.getId(), builder -> builder.creditorContains(insecure().nextAlphanumeric(10)));
        List<CategorySelector> selectors = category.getSelectors();

        // when: the parent category is saved
        fixture.save(category);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: all selectors are saved
        selectors.forEach(selector -> assertNotNull(selector.getId()));

        // when: the parent category is deleted
        fixture.delete(category);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the category is deleted
        assertTrue(fixture.findByIdOptional(category.getId()).isEmpty());

        // and: all selectors are deleted
        assertEquals(0, fixture.getEntityManager()
            .createNativeQuery("select count(*) from category_selector where category_id = :categoryId")
            .setParameter("categoryId", category.getId())
            .getFirstResult());
    }

    @Test
    public void testFindByGroupIdPaged() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: a category group for the user
        CategoryGroup categoryGroup = categoryGroupRepository.save(CategoryGroup.builder()
            .userId(userId)
            .name(insecure().nextAlphanumeric(30))
            .description(insecure().nextAlphanumeric(30))
            .build());

        // and: the group has a collection of categories
        List<Category> userCategories = IntStream.range(0, 30)
            .mapToObj(index ->
                Category.builder()
                    .group(categoryGroup)
                    .name(insecure().nextAlphanumeric(30))
                    .description(insecure().nextAlphanumeric(30))
                    .colour("#FF0000")
                    .build()
            )
            .sorted(Comparator.comparing(Category::getName, String::compareToIgnoreCase))
            .toList();

        // and: the categories are persisted
        fixture.saveAll(userCategories);
        fixture.flush();
        fixture.getEntityManager().clear();

        // when: retrieving categories by page
        int pageSize = 10;
        int pageCount = 3;
        for (int page = 0; page < pageCount; page++) {
            List<Category> categories = fixture.findByGroupId(categoryGroup.getId(), page, pageSize).getContent();

            // then: the categories are retrieved for that page
            assertEquals(pageSize, categories.size());
            assertEquals(userCategories.subList(page * pageSize, (page + 1) * pageSize), categories);
        }
    }

    private UserConsent mockUserConsent() {
        return TestData.mockUserConsent(UUID.randomUUID(), consent -> consent.id(null));
    }

    private Account mockAccount(UserConsent consent) {
        return TestData.mockAccount(consent.getUserId(), account -> {
            account.id(null);
            account.userConsentId(consent.getId());
            account.institutionId(consent.getInstitutionId());
        });
    }
}
