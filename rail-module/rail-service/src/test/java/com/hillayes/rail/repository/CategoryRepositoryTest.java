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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
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
    public void testSave() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // when: persisting the category
        Category category = fixture.save(Category.builder()
            .userId(consent.getUserId())
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
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

        // and: a category
        Category category = Category.builder()
            .userId(consent.getUserId())
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
            .colour("#FF0000")
            .build();

        // when: selectors are added
        List<CategorySelector> selectors = List.of(
            category.addSelector(account.getId(), builder -> {
                builder.infoContains(randomAlphanumeric(10));
                builder.refContains(randomAlphanumeric(20));
                builder.creditorContains(randomAlphanumeric(15));
            }),
            category.addSelector(account.getId(), builder ->
                builder.infoContains(randomAlphanumeric(12))
            ),
            category.addSelector(account.getId(), builder ->
                builder.refContains(randomAlphanumeric(25))
            ),
            category.addSelector(account.getId(), builder ->
                builder.creditorContains(randomAlphanumeric(10))
            )
        );

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
    public void testDelete() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category
        Category category = Category.builder()
            .userId(consent.getUserId())
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
            .colour("#FF0000")
            .build();

        // when: selectors are added to the category
        List<CategorySelector> selectors = List.of(
            category.addSelector(account.getId(), builder -> builder.infoContains(randomAlphanumeric(20))),
            category.addSelector(account.getId(), builder -> builder.refContains(randomAlphanumeric(5))),
            category.addSelector(account.getId(), builder -> builder.creditorContains(randomAlphanumeric(10)))
        );

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
    public void testFindByUserId() {
        // given: a collection of user identities
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // and: each user has a collection of categories
        Map<UUID, List<Category>> userCategories = userIds.stream()
            .flatMap(userId -> IntStream.range(0, 3).mapToObj(index ->
                fixture.save(Category.builder()
                    .userId(userId)
                    .name(randomAlphanumeric(30))
                    .description(randomAlphanumeric(30))
                    .colour("#FF0000")
                    .build()))
            )
            .collect(Collectors.groupingBy(Category::getUserId));

        // and: the categories are persisted
        fixture.flush();
        fixture.getEntityManager().clear();

        // when: retrieving all categories for each user
        userIds.forEach(userId -> {
            List<Category> categories = fixture.findByUserId(userId, 0, 10).getContent();

            // then: the categories are retrieved
            assertEquals(userCategories.get(userId).size(), categories.size());
            userCategories.get(userId).forEach(category -> assertTrue(categories.contains(category)));
        });
    }

    @Test
    public void testFindByUserIdPaged() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user has a collection of categories
        List<Category> userCategories = IntStream.range(0, 30)
            .mapToObj(index ->
                Category.builder()
                    .userId(userId)
                    .name(randomAlphanumeric(30))
                    .description(randomAlphanumeric(30))
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
            List<Category> categories = fixture.findByUserId(userId, page, pageSize).getContent();

            // then: the categories are retrieved for that page
            assertEquals(pageSize, categories.size());
            assertEquals(userCategories.subList(page * pageSize, (page + 1) * pageSize), categories);
        }
    }

    @Test
    public void testDeleteByUserId() {
        // given: a collection of user identities
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // and: each user has a collection of categories
        Map<UUID, List<Category>> userCategories = userIds.stream()
            .flatMap(userId -> IntStream.range(0, 3).mapToObj(index ->
                fixture.save(Category.builder()
                    .userId(userId)
                    .name(randomAlphanumeric(30))
                    .description(randomAlphanumeric(30))
                    .colour("#FF0000")
                    .build()))
            )
            .collect(Collectors.groupingBy(Category::getUserId));

        // and: the categories are persisted
        fixture.flush();
        fixture.getEntityManager().clear();
        userIds.forEach(userId ->
            userCategories.get(userId)
                .forEach(category -> assertTrue(fixture.findByIdOptional(category.getId()).isPresent()))
        );

        // when: deleting all categories for user #2
        fixture.deleteByUserId(userIds.get(1));
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the categories for user #2 are deleted
        userCategories.get(userIds.get(1))
            .forEach(category -> assertTrue(fixture.findByIdOptional(category.getId()).isEmpty()));

        // and: the categories for user #1 are not deleted
        userCategories.get(userIds.get(0))
            .forEach(category -> assertTrue(fixture.findByIdOptional(category.getId()).isPresent()));

        // and: the categories for user #3 are not deleted
        userCategories.get(userIds.get(2))
            .forEach(category -> assertTrue(fixture.findByIdOptional(category.getId()).isPresent()));
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
