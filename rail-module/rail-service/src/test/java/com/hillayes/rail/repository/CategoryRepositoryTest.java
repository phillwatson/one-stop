package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    AccountTransactionRepository accountTransactionRepository;

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
            category.addSelector(account.getId(), builder -> {
                builder.infoContains(randomAlphanumeric(10));
                builder.refContains(randomAlphanumeric(20));
                builder.creditorContains(randomAlphanumeric(15));
            });
            category.addSelector(account.getId(), builder ->
                builder.infoContains(randomAlphanumeric(12))
            );
            category.addSelector(account.getId(), builder ->
                builder.refContains(randomAlphanumeric(25))
            );
            category.addSelector(account.getId(), builder ->
                builder.creditorContains(randomAlphanumeric(10))
            );
        Set<CategorySelector> selectors = category.getSelectors();

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
        category.addSelector(account.getId(), builder -> builder.infoContains(randomAlphanumeric(20)));
        category.addSelector(account.getId(), builder -> builder.refContains(randomAlphanumeric(5)));
        category.addSelector(account.getId(), builder -> builder.creditorContains(randomAlphanumeric(10)));
        Set<CategorySelector> selectors = category.getSelectors();

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
    public void testFindByUserAndName() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: several categories
        List<Category> categories = IntStream.range(0, 5).mapToObj(index ->
            fixture.save(Category.builder()
                .userId(consent.getUserId())
                .name(randomAlphanumeric(30))
                .description(randomAlphanumeric(30))
                .colour("#FF0000")
                .build())
        ).toList();

        fixture.flush();
        fixture.getEntityManager().clear();

        // when: retrieving each category by user and name
        categories.forEach(category -> {
            Optional<Category> result = fixture.findByUserAndName(consent.getUserId(), category.getName());

            // then: the named category is retrieved
            assertTrue(result.isPresent());
            assertEquals(category.getId(), result.get().getId());
        });
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

    @Test
    public void testGetStatistics() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a collection of categories with selectors for the account's transactions
        List<Category> categories = List.of(
            Category.builder().userId(consent.getUserId()).name("category 1").build()
                .addSelector(account.getId(), builder -> builder.infoContains("info 1").build()),
            Category.builder().userId(consent.getUserId()).name("category 2").build()
                .addSelector(account.getId(), builder -> builder.refContains("info 2").build()),
            Category.builder().userId(consent.getUserId()).name("category 3").build()
                .addSelector(account.getId(), builder -> builder.creditorContains("info 3").build()),
            Category.builder().userId(consent.getUserId()).name("category 4").build()
                .addSelector(account.getId(), builder -> builder.infoContains("info 4").build())
        );
        fixture.saveAll(categories);

        // and: a collection of transactions for the account
        // and: a match for each selector
        // and: one that doesn't match the selectors
        accountTransactionRepository.saveAll(List.of(
            TestData.mockAccountTransaction(account, transaction ->
                transaction.id(null)
                    .bookingDateTime(now.minus(Duration.ofDays(7)))
                    .additionalInformation("contains info 1 text")
                    .amount(MonetaryAmount.of("GBP", 100))
            ),
            TestData.mockAccountTransaction(account, transaction ->
                transaction.id(null)
                    .bookingDateTime(now.minus(Duration.ofDays(6)))
                    .reference("contains info 2 text")
                    .amount(MonetaryAmount.of("GBP", 200))
            ),
            TestData.mockAccountTransaction(account, transaction ->
                transaction.id(null)
                    .bookingDateTime(now.minus(Duration.ofDays(5)))
                    .creditorName("contains info 3 text")
                    .amount(MonetaryAmount.of("GBP", 300))
            ),
            TestData.mockAccountTransaction(account, transaction ->
                transaction.id(null)
                    .bookingDateTime(now.minus(Duration.ofDays(4)))
                    .additionalInformation("contains info 4 text")
                    .amount(MonetaryAmount.of("GBP", 400))
            ),
            TestData.mockAccountTransaction(account, transaction ->
                transaction.id(null)
                    .bookingDateTime(now.minus(Duration.ofDays(3)))
                    .additionalInformation("contains uncategorised text")
                    .amount(MonetaryAmount.of("GBP", 500))
            )
        ));
        fixture.flush();

        // when: the statistics are retrieved
        List<CategoryStatistics> statistics =
            fixture.getStatistics(consent.getUserId(), now.minus(Duration.ofDays(7)), now);

        // then: the statistics are retrieved
        assertEquals(5, statistics.size());

        // and: the statistics are correct
        categories.forEach(category -> {
            CategoryStatistics projection = statistics.stream()
                .filter(stat -> stat.getCategory().equals(category.getName()))
                .findFirst()
                .orElseThrow();

            assertEquals(category.getId(), projection.getCategoryId());
            assertEquals(1, projection.getCount());
        });

        // and: the uncategorised statistic is correct
        statistics.stream().filter(stat -> stat.getCategoryId() == null).findFirst().ifPresent(stat ->
            assertEquals(5.00, stat.getTotal().doubleValue())
        );
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
