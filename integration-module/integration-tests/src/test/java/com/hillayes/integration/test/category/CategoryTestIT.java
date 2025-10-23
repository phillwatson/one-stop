package com.hillayes.integration.test.category;

import com.google.common.collect.Streams;
import com.hillayes.integration.api.*;
import com.hillayes.integration.api.admin.RailAgreementAdminApi;
import com.hillayes.integration.api.admin.RailRequisitionAdminApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionStatus;
import com.hillayes.onestop.api.*;
import com.hillayes.sim.email.SendInBlueSimulator;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryTestIT extends ApiTestBase {
    @Test
    public void testCategoryGroups() {
        // given: a user
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserEntity.builder()
            .username(randomStrings.nextAlphanumeric(20))
            .givenName(randomStrings.nextAlphanumeric(10))
            .password(randomStrings.nextAlphanumeric(30))
            .email(randomStrings.nextAlphanumeric(30))
            .build());

        CategoryApi categoryApi = new CategoryApi(user.getAuthTokens());

        // when: the user creates a category group
        CategoryGroupRequest createGroupRequest = new CategoryGroupRequest()
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50));
        CategoryGroupResponse group = categoryApi.createCategoryGroup(createGroupRequest);

        // then: the new group is returned
        assertNotNull(group);
        assertNotNull(group.getId());
        assertEquals(createGroupRequest.getName(), group.getName());
        assertEquals(createGroupRequest.getDescription(), group.getDescription());

        // when: the category group is updated
        CategoryGroupRequest updateGroupRequest = new CategoryGroupRequest()
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50));
        categoryApi.updateCategoryGroup(group.getId(), updateGroupRequest);

        // and: the updated category group is retrieved
        CategoryGroupResponse updatedGroup = categoryApi.getCategoryGroup(group.getId());

        // then: the category group is returned
        assertEquals(group.getId(), updatedGroup.getId());
        assertEquals(updateGroupRequest.getName(), updatedGroup.getName());
        assertEquals(updateGroupRequest.getDescription(), updatedGroup.getDescription());

        // when: 19 more category groups are created
        List<UUID> allGroupIds = new ArrayList<>();
        allGroupIds.add(group.getId());
        allGroupIds.addAll(IntStream.range(0, 19)
            .mapToObj(i -> new CategoryGroupRequest()
                .name(randomStrings.nextAlphanumeric(20))
                .description(randomStrings.nextAlphanumeric(50))
            )
            .map(request -> categoryApi.createCategoryGroup(request).getId())
            .toList()
        );

        // and: a page of ALL category groups are retrieved
        PaginatedCategoryGroups categoryGroups = categoryApi.getCategoryGroups(0, 20);

        // then: the page counts are correct
        assertEquals(0, categoryGroups.getPage());
        assertEquals(20, categoryGroups.getPageSize());
        assertEquals(20, categoryGroups.getCount());
        assertEquals(20, categoryGroups.getTotal());
        assertEquals(1, categoryGroups.getTotalPages());

        // and: the page contains all category groups
        assertNotNull(categoryGroups.getItems());
        assertEquals(20, categoryGroups.getItems().size());
        allGroupIds.forEach(id ->
            assertTrue(categoryGroups.getItems().stream().anyMatch(g -> g.getId().equals(id)))
        );
    }

    @Test
    public void testCategories() {
        // given: a user
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserEntity.builder()
            .username(randomStrings.nextAlphanumeric(20))
            .givenName(randomStrings.nextAlphanumeric(10))
            .password(randomStrings.nextAlphanumeric(30))
            .email(randomStrings.nextAlphanumeric(30))
            .build());

        CategoryApi categoryApi = new CategoryApi(user.getAuthTokens());

        // and: the user creates a category group
        CategoryGroupResponse group = categoryApi.createCategoryGroup(new CategoryGroupRequest()
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50)));
        assertNotNull(group);

        // when: the user creates a category within the group
        CategoryRequest createCategoryRequest = new CategoryRequest()
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50))
            .colour("#112233");
        CategoryResponse category = categoryApi.createCategory(group.getId(), createCategoryRequest);

        // then: the new category is returned
        assertNotNull(category.getId());
        assertEquals(createCategoryRequest.getName(), category.getName());
        assertEquals(createCategoryRequest.getDescription(), category.getDescription());
        assertEquals(createCategoryRequest.getColour(), category.getColour());

        // when: the category is updated
        CategoryRequest updateCategoryRequest = new CategoryRequest()
            .name(randomStrings.nextAlphanumeric(20))
            .description(randomStrings.nextAlphanumeric(50))
            .colour("#445566");
        categoryApi.updateCategory(category.getId(), updateCategoryRequest);

        // and: the updated category is retrieved
        CategoryResponse updatedCategory = categoryApi.getCategory(category.getId());

        // then: the category is returned
        assertEquals(category.getId(), updatedCategory.getId());
        assertEquals(group.getId(), updatedCategory.getGroupId());
        assertEquals(updateCategoryRequest.getName(), updatedCategory.getName());
        assertEquals(updateCategoryRequest.getDescription(), updatedCategory.getDescription());
        assertEquals(updateCategoryRequest.getColour(), updatedCategory.getColour());

        // when: 19 more categories are created
        List<UUID> allCategoryIds = new ArrayList<>();
        allCategoryIds.add(category.getId());
        allCategoryIds.addAll(IntStream.range(0, 19)
            .mapToObj(i -> new CategoryRequest()
                .name(randomStrings.nextAlphanumeric(20))
                .description(randomStrings.nextAlphanumeric(50))
                .colour("#778899")
            )
            .map(request -> categoryApi.createCategory(group.getId(), request).getId())
            .toList()
        );

        // and: a page of ALL category are retrieved for the group
        PaginatedCategories categories = categoryApi.getCategories(group.getId(), 0, 20);

        // then: the page counts are correct
        assertEquals(0, categories.getPage());
        assertEquals(20, categories.getPageSize());
        assertEquals(20, categories.getCount());
        assertEquals(20, categories.getTotal());
        assertEquals(1, categories.getTotalPages());

        // and: the page contains all category
        assertNotNull(categories.getItems());
        assertEquals(20, categories.getItems().size());
        allCategoryIds.forEach(id ->
            assertTrue(categories.getItems().stream().anyMatch(cat -> cat.getId().equals(id)))
        );

        // when: the category is deleted
        categoryApi.deleteCategory(category.getId());

        // then: the category can no longer be retrieved
        withServiceError(categoryApi.getCategory(category.getId(), 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("Category", error.getContextAttributes().get("entity-type"));
            assertEquals(category.getId().toString(), error.getContextAttributes().get("entity-id"));
        });

        // when: the category group is deleted
        categoryApi.deleteCategoryGroup(group.getId());

        // then: the category group can no longer be retrieved
        withServiceError(categoryApi.getCategoryGroup(group.getId(), 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("CategoryGroup", error.getContextAttributes().get("entity-type"));
            assertEquals(group.getId().toString(), error.getContextAttributes().get("entity-id"));
        });

        // and: none of its categories can be retrieved
        allCategoryIds.forEach(id ->
            categoryApi.getCategory(id, 404)
        );
    }

    @Test
    public void testCategorySelectors() {
        // given: the admin user signs in
        AuthApi authApi = new AuthApi();
        Map<String, String> adminAuthTokens = authApi.login("admin", "password");
        assertNotNull(adminAuthTokens);
        assertEquals(3, adminAuthTokens.size());

        // and: a user is created
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserEntity.builder()
            .username(randomStrings.nextAlphanumeric(20))
            .givenName(randomStrings.nextAlphanumeric(10))
            .password(randomStrings.nextAlphanumeric(30))
            .email(randomStrings.nextAlphanumeric(30))
            .build());

        // and: the user creates accounts - and the first account is selected
        UUID accountId = createAccounts(adminAuthTokens, user).get(0);

        CategoryApi categoryApi = new CategoryApi(user.getAuthTokens());

        // and: the user creates a category group
        CategoryGroupResponse group = categoryApi.createCategoryGroup(new CategoryGroupRequest()
            .name(randomStrings.nextAlphanumeric(20)));
        assertNotNull(group);

        // and: the user creates a category within the group
        CategoryResponse category = categoryApi.createCategory(group.getId(), new CategoryRequest()
            .name(randomStrings.nextAlphanumeric(20)));
        assertNotNull(category);

        // when: the user creates category selectors
        List<AccountCategorySelector> setSelectorsRequest = IntStream.range(0, 10)
            .mapToObj(i -> new AccountCategorySelector()
                .creditorContains(randomStrings.nextAlphanumeric(10))
                .refContains(randomStrings.nextAlphanumeric(10))
                .infoContains(randomStrings.nextAlphanumeric(10))
            ).toList();
        List<AccountCategorySelector> selectors =
            categoryApi.setAccountCategorySelectors(category.getId(), accountId, setSelectorsRequest);

        // then: the new selectors are returned
        compare(setSelectorsRequest, selectors);

        // when: the selectors are retrieved
        selectors = categoryApi.getAccountCategorySelectors(category.getId(), accountId);

        // then: the selectors are returned
        compare(setSelectorsRequest, selectors);

        // when: the selectors are modified
        // take first 5 original and add 10 new
        List<AccountCategorySelector> updateSelectorsRequest = Streams.concat(
                setSelectorsRequest.stream().limit(5),
                IntStream.range(0, 10).mapToObj(i -> new AccountCategorySelector()
                    .creditorContains(randomStrings.nextAlphanumeric(10))
                    .refContains(randomStrings.nextAlphanumeric(10))
                    .infoContains(randomStrings.nextAlphanumeric(10))
                )
            )
            .toList();

        selectors = categoryApi.setAccountCategorySelectors(category.getId(), accountId, updateSelectorsRequest);

        // then: the updated selectors are returned
        compare(updateSelectorsRequest, selectors);

        // when: the updated selectors are retrieved
        selectors = categoryApi.getAccountCategorySelectors(category.getId(), accountId);

        // then: the updated selectors are returned
        compare(updateSelectorsRequest, selectors);

        // when: the category is deleted
        categoryApi.deleteCategory(category.getId());

        // then: the category can no longer be retrieved
        withServiceError(categoryApi.getCategory(category.getId(), 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("Category", error.getContextAttributes().get("entity-type"));
            assertEquals(category.getId().toString(), error.getContextAttributes().get("entity-id"));
        });

        // and: the selectors can no longer be retrieved
        withServiceError(categoryApi.getAccountCategorySelectors(category.getId(), accountId, 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("Category", error.getContextAttributes().get("entity-type"));
            assertEquals(category.getId().toString(), error.getContextAttributes().get("entity-id"));
        });
    }

    private void compare(Collection<AccountCategorySelector> expected,
                         Collection<AccountCategorySelector> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        expected.forEach(request ->
            assertTrue(actual.stream().anyMatch(selector ->
                Objects.equals(request.getCreditorContains(), selector.getCreditorContains()) &&
                Objects.equals(request.getRefContains(), selector.getRefContains()) &&
                Objects.equals(request.getInfoContains(), selector.getInfoContains())
            ))
        );
    }

    private List<UUID> createAccounts(Map<String, String> adminAuthTokens, UserEntity user) {
        // establish authenticated APIs
        RailAgreementAdminApi agreementAdminApi = new RailAgreementAdminApi(adminAuthTokens);
        RailRequisitionAdminApi requisitionAdminApi = new RailRequisitionAdminApi(adminAuthTokens);
        InstitutionApi institutionApi = new InstitutionApi(user.getAuthTokens());
        UserConsentApi userConsentApi = new UserConsentApi(user.getAuthTokens());
        AccountApi accountApi = new AccountApi(user.getAuthTokens());

        // and: the user can identify the institution
        InstitutionResponse institution = institutionApi.getInstitution("SANDBOXFINANCE_SFIN0000");
        assertNotNull(institution);

        // when: the user initiates a consent request for the institution
        URI callbackUri = URI.create("http://mock/callback/uri");
        userConsentApi.register(institution.getId(), new UserConsentRequest()
            .callbackUri(callbackUri));

        // and: a user-consent record is waiting to be given
        UserConsentResponse userConsent = userConsentApi.getConsentForInstitution(institution.getId());
        assertEquals("INITIATED", userConsent.getStatus());

        // and: an end-user agreement is created for the institution
        PaginatedList<EndUserAgreement> agreements = agreementAdminApi.list(0, 100);
        assertNotNull(agreements);
        assertEquals(1, agreements.count); // only one as we clear data on each test

        // and: a requisition record is created - only one as we clear data on each test
        PaginatedList<Requisition> requisitions = requisitionAdminApi.list(0, 100);
        assertEquals(1, requisitions.count);
        Requisition requisition = requisitions.results.get(0);

        // when: the requisition process is complete
        while (requisition.status != RequisitionStatus.LN) {
            requisition = requisitionAdminApi.get(requisition.id);
        }

        // then: the requisitioned accounts are identified
        assertFalse(requisition.accounts.isEmpty());

        try (SendInBlueSimulator emailSim = new SendInBlueSimulator(getWiremockPort())) {
            // when: the success response is returned from the rails service
            Response response = userConsentApi.consentResponse(institution.getProvider(), requisition.reference, null, null);

            // then: the redirect response is the original callback URI
            assertEquals(callbackUri.toString(), response.getHeader("Location"));

            // and: a confirmation email is sent to the user
            emailSim.verifyEmailSent(user.getEmail(), "Your One-Stop access to " + institution.getName(),
                await().atMost(Duration.ofSeconds(60)));

            // and: the user can retrieve their consent record
            UserConsentResponse consentForInstitution = userConsentApi.getConsentForInstitution(institution.getId());
            assertNotNull(consentForInstitution);

            // and: consent status shows it has been given
            assertEquals("GIVEN", consentForInstitution.getStatus());

            // and: no error was recorded during consent
            assertNull(consentForInstitution.getErrorCode());
        }

        // when: the user's accounts have been polled by the service
        int accountCount = requisition.accounts.size();
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() ->
                assertEquals(accountCount, accountApi.getAccounts(0, accountCount).getCount())
            );

        return accountApi.getAccounts(0, 5)
            .getItems().stream()
            .map(AccountResponse::getId)
            .toList();
    }
}
