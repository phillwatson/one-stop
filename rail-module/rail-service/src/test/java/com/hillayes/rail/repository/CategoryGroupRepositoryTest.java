package com.hillayes.rail.repository;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.rail.config.ServiceConfiguration;
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
public class CategoryGroupRepositoryTest {
    @Inject
    ServiceConfiguration serviceConfiguration;

    @Inject
    UserConsentRepository userConsentRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    @Inject
    CategoryGroupRepository fixture;

    @Test
    public void testSave() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // when: persisting the category
        CategoryGroup category = fixture.save(CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
            .build());

        fixture.flush();

        // then: the category is persisted
        assertNotNull(category.getId());
    }

    @Test
    public void testSaveCategories() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a category
        CategoryGroup categoryGroup = CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
            .build();

        // when: selectors are added
        categoryGroup.addCategory(randomAlphanumeric(10), builder -> {
            builder.description(randomAlphanumeric(20));
            builder.colour("#FF0000");
        });
        categoryGroup.addCategory(randomAlphanumeric(10), builder -> {
            builder.description(randomAlphanumeric(20));
            builder.colour("#FF0001");
        });
        categoryGroup.addCategory(randomAlphanumeric(10), builder -> {
            builder.description(randomAlphanumeric(20));
            builder.colour("#FF0002");
        });

        Set<Category> categories = categoryGroup.getCategories();

        // then: the selectors are linked to the category group
        categories.forEach(category -> assertEquals(categoryGroup, category.getGroup()));

        // when: the category is persisted
        fixture.save(categoryGroup);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the selectors are persisted
        categories.forEach(selector -> assertNotNull(selector.getId()));

        // when: the category group is retrieved
        CategoryGroup retrieved = fixture.findByIdOptional(categoryGroup.getId()).orElseThrow();

        // then: the selectors are retrieved
        assertEquals(categories.size(), retrieved.getCategories().size());
    }

    @Test
    public void testDelete() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a category
        CategoryGroup categoryGroup = CategoryGroup.builder()
            .userId(consent.getUserId())
            .name(randomAlphanumeric(30))
            .description(randomAlphanumeric(30))
            .build();

        // when: categories are added to the category group
        categoryGroup.addCategory(randomAlphanumeric(20), builder -> builder.description(randomAlphanumeric(20)));
        categoryGroup.addCategory(randomAlphanumeric(20), builder -> builder.description(randomAlphanumeric(5)));
        categoryGroup.addCategory(randomAlphanumeric(20), builder -> builder.description(randomAlphanumeric(10)));
        Set<Category> categories = categoryGroup.getCategories();

        // when: the category group is saved
        fixture.save(categoryGroup);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: all categories are saved
        categories.forEach(category -> assertNotNull(category.getId()));

        // when: the category group is deleted
        fixture.delete(categoryGroup);
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the category group is deleted
        assertTrue(fixture.findByIdOptional(categoryGroup.getId()).isEmpty());

        // and: all categories are deleted
        assertEquals(0, fixture.getEntityManager()
            .createNativeQuery("select count(*) from category where group_id = :groupId")
            .setParameter("groupId", categoryGroup.getId())
            .getFirstResult());
    }

    @Test
    public void testFindByUserId() {
        // given: a collection of user identities
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // and: each user has a collection of category groups
        Map<UUID, List<CategoryGroup>> userCategoryGroups = userIds.stream()
            .flatMap(userId -> IntStream.range(0, 3).mapToObj(index ->
                fixture.save(CategoryGroup.builder()
                    .userId(userId)
                    .name(randomAlphanumeric(30))
                    .description(randomAlphanumeric(30))
                    .build()))
            )
            .collect(Collectors.groupingBy(CategoryGroup::getUserId));

        // and: the category groups are persisted
        fixture.flush();
        fixture.getEntityManager().clear();

        // when: retrieving all category groups for each user
        userIds.forEach(userId -> {
            List<CategoryGroup> groups = fixture.findByUserId(userId, 0, 10).getContent();

            // then: the categories are retrieved
            assertEquals(userCategoryGroups.get(userId).size(), groups.size());
            userCategoryGroups.get(userId).forEach(category -> assertTrue(groups.contains(category)));
        });
    }

    @Test
    public void testFindByUserIdPaged() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user has a collection of category groups
        List<CategoryGroup> userCategoryGroups = IntStream.range(0, 30)
            .mapToObj(index ->
                CategoryGroup.builder()
                    .userId(userId)
                    .name(randomAlphanumeric(30))
                    .description(randomAlphanumeric(30))
                    .build()
            )
            .sorted(Comparator.comparing(CategoryGroup::getName, String::compareToIgnoreCase))
            .toList();

        // and: the category groups are persisted
        fixture.saveAll(userCategoryGroups);
        fixture.flush();
        fixture.getEntityManager().clear();

        // when: retrieving category group by page
        int pageSize = 10;
        int pageCount = 3;
        for (int page = 0; page < pageCount; page++) {
            List<CategoryGroup> categoryGroups = fixture.findByUserId(userId, page, pageSize).getContent();

            // then: the category groups are retrieved for that page
            assertEquals(pageSize, categoryGroups.size());
            assertEquals(userCategoryGroups.subList(page * pageSize, (page + 1) * pageSize), categoryGroups);
        }
    }

    @Test
    public void testFindByUserAndName() {
        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: several categories
        List<CategoryGroup> categoryGroups = IntStream.range(0, 5).mapToObj(index ->
            fixture.save(CategoryGroup.builder()
                .userId(consent.getUserId())
                .name(randomAlphanumeric(30))
                .description(randomAlphanumeric(30))
                .build())
        ).toList();

        fixture.flush();
        fixture.getEntityManager().clear();

        // when: retrieving each category by user and name
        categoryGroups.forEach(categoryGroup -> {
            Optional<CategoryGroup> result = fixture.findByUserAndName(consent.getUserId(), categoryGroup.getName());

            // then: the named category is retrieved
            assertTrue(result.isPresent());
            assertEquals(categoryGroup.getId(), result.get().getId());
        });
    }

    @Test
    public void testDeleteByUserId() {
        // given: a collection of user identities
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // and: each user has a collection of category groups
        Map<UUID, List<CategoryGroup>> userCategories = userIds.stream()
            .flatMap(userId -> IntStream.range(0, 3).mapToObj(index ->
                fixture.save(CategoryGroup.builder()
                    .userId(userId)
                    .name(randomAlphanumeric(30))
                    .description(randomAlphanumeric(30))
                    .build()))
            )
            .collect(Collectors.groupingBy(CategoryGroup::getUserId));

        // and: the category groups are persisted
        fixture.flush();
        fixture.getEntityManager().clear();
        userIds.forEach(userId ->
            userCategories.get(userId)
                .forEach(category -> assertTrue(fixture.findByIdOptional(category.getId()).isPresent()))
        );

        // when: deleting all category groups for user #2
        fixture.deleteByUserId(userIds.get(1));
        fixture.flush();
        fixture.getEntityManager().clear();

        // then: the category groups for user #2 are deleted
        userCategories.get(userIds.get(1))
            .forEach(group -> assertTrue(fixture.findByIdOptional(group.getId()).isEmpty()));

        // and: the category groups for user #1 are not deleted
        userCategories.get(userIds.get(0))
            .forEach(group -> assertTrue(fixture.findByIdOptional(group.getId()).isPresent()));

        // and: the category groups for user #3 are not deleted
        userCategories.get(userIds.get(2))
            .forEach(group -> assertTrue(fixture.findByIdOptional(group.getId()).isPresent()));
    }

    @Test
    public void testGetStatistics() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // given: a user-consent
        UserConsent consent = userConsentRepository.save(mockUserConsent());

        // and: a linked account
        Account account = accountRepository.save(mockAccount(consent));

        // and: a category group
        CategoryGroup categoryGroup = CategoryGroup.builder()
            .userId(consent.getUserId())
            .name("group 1")
            .description("desc 1")
            .build();

        // and: a collection of categories with selectors for the account's transactions
        List<Category> categories = List.of(
            categoryGroup.addCategory("category 1", builder -> builder.description("desc 1").colour("#111111").build())
                .addSelector(account.getId(), builder -> builder.infoContains("info 1").build()),
            categoryGroup.addCategory("category 2", builder -> builder.description("desc 2").colour("#222222").build())
                .addSelector(account.getId(), builder -> builder.refContains("info 2").build()),
            categoryGroup.addCategory("category 3", builder -> builder.description("desc 3").colour("#333333").build())
                .addSelector(account.getId(), builder -> builder.creditorContains("info 3").build()),
            categoryGroup.addCategory("category 4", builder -> builder.description("desc 4").colour("#444444").build())
                .addSelector(account.getId(), builder -> builder.infoContains("info 4").build())
        );

        fixture.save(categoryGroup);

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
            fixture.getStatistics(categoryGroup, now.minus(Duration.ofDays(7)), now);

        // then: the statistics are retrieved
        assertEquals(5, statistics.size());

        // and: the statistics are correct
        categories.forEach(category -> {
            CategoryStatistics projection = statistics.stream()
                .filter(stat -> stat.getCategory().equals(category.getName()))
                .findFirst()
                .orElseThrow();

            assertEquals(categoryGroup.getId(), projection.getGroupId());
            assertEquals(categoryGroup.getName(), projection.getGroupName());
            assertEquals(category.getId(), projection.getCategoryId());
            assertEquals(1, projection.getCount());
        });

        // and: the uncategorised statistic is correct
        String defaultName = serviceConfiguration.categories().uncategorisedName();
        String defaultColour = serviceConfiguration.categories().defaultColour();
        statistics.stream()
            .filter(stat -> stat.getCategoryId() == null)
            .findFirst()
            .ifPresent(stat -> {
                assertEquals(defaultName, stat.getCategory());
                assertEquals(defaultColour, stat.getColour());
                assertEquals(5.00, stat.getTotal().doubleValue());
                assertEquals(1, stat.getCount());
            });
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
