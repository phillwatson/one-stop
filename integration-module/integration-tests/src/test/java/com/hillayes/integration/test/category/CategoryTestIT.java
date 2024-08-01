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
import com.hillayes.sim.email.SendWithBlueSimulator;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryTestIT extends ApiTestBase {
    @Test
    public void testCategoryGroups() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        CategoryApi categoryApi = new CategoryApi(user.getAuthTokens());

        // when: the user creates a category group
        CategoryGroupRequest createGroupRequest = new CategoryGroupRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(50));
        UUID groupId = categoryApi.createCategoryGroup(createGroupRequest);

        // then: the ID of the new group is returned
        assertNotNull(groupId);

        // when: the category group is retrieved
        CategoryGroupResponse categoryGroup = categoryApi.getCategoryGroup(groupId);

        // then: the category group is returned
        assertEquals(groupId, categoryGroup.getId());
        assertEquals(createGroupRequest.getName(), categoryGroup.getName());
        assertEquals(createGroupRequest.getDescription(), categoryGroup.getDescription());

        // when: the category group is updated
        CategoryGroupRequest updateGroupRequest = new CategoryGroupRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(50));
        categoryApi.updateCategoryGroup(groupId, updateGroupRequest);

        // and: the updated category group is retrieved
        CategoryGroupResponse updatedGroup = categoryApi.getCategoryGroup(groupId);

        // then: the category group is returned
        assertEquals(groupId, updatedGroup.getId());
        assertEquals(updateGroupRequest.getName(), updatedGroup.getName());
        assertEquals(updateGroupRequest.getDescription(), updatedGroup.getDescription());

        // when: 19 more category groups are created
        List<UUID> allGroupIds = new ArrayList<>();
        allGroupIds.add(groupId);
        allGroupIds.addAll(IntStream.range(0, 19)
            .mapToObj(i -> new CategoryGroupRequest()
                .name(randomAlphanumeric(20))
                .description(randomAlphanumeric(50))
            )
            .map(categoryApi::createCategoryGroup)
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
            assertTrue(categoryGroups.getItems().stream().anyMatch(group -> group.getId().equals(id)))
        );
    }

    @Test
    public void testCategories() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        CategoryApi categoryApi = new CategoryApi(user.getAuthTokens());

        // and: the user creates a category group
        CategoryGroupRequest createGroupRequest = new CategoryGroupRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(50));
        UUID groupId = categoryApi.createCategoryGroup(createGroupRequest);
        assertNotNull(groupId);

        // when: the user creates a category within the group
        CategoryRequest createCategoryRequest = new CategoryRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(50))
            .colour("#112233");

        // then: the ID of the new category is returned
        UUID categoryId = categoryApi.createCategory(groupId, createCategoryRequest);

        // when: the category is retrieved
        CategoryResponse category = categoryApi.getCategory(categoryId);

        // then: the category is returned
        assertEquals(categoryId, category.getId());
        assertEquals(createCategoryRequest.getName(), category.getName());
        assertEquals(createCategoryRequest.getDescription(), category.getDescription());
        assertEquals(createCategoryRequest.getColour(), category.getColour());

        // when: the category is updated
        CategoryRequest updateCategoryRequest = new CategoryRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(50))
            .colour("#445566");
        categoryApi.updateCategory(categoryId, updateCategoryRequest);

        // and: the updated category is retrieved
        CategoryResponse updatedCategory = categoryApi.getCategory(categoryId);

        // then: the category is returned
        assertEquals(categoryId, updatedCategory.getId());
        assertEquals(groupId, updatedCategory.getGroupId());
        assertEquals(updateCategoryRequest.getName(), updatedCategory.getName());
        assertEquals(updateCategoryRequest.getDescription(), updatedCategory.getDescription());
        assertEquals(updateCategoryRequest.getColour(), updatedCategory.getColour());

        // when: 19 more categories are created
        List<UUID> allCategoryIds = new ArrayList<>();
        allCategoryIds.add(categoryId);
        allCategoryIds.addAll(IntStream.range(0, 19)
            .mapToObj(i -> new CategoryRequest()
                .name(randomAlphanumeric(20))
                .description(randomAlphanumeric(50))
                .colour("#778899")
            )
            .map(request -> categoryApi.createCategory(groupId, request))
            .toList()
        );

        // and: a page of ALL category are retrieved for the group
        PaginatedCategories categories = categoryApi.getCategories(groupId, 0, 20);

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
        categoryApi.deleteCategory(categoryId);

        // then: the category can no longer be retrieved
        withServiceError(categoryApi.getCategory(categoryId, 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("Category", error.getContextAttributes().get("entity-type"));
            assertEquals(categoryId.toString(), error.getContextAttributes().get("entity-id"));
        });

        // when: the category group is deleted
        categoryApi.deleteCategoryGroup(groupId);

        // then: the category group can no longer be retrieved
        withServiceError(categoryApi.getCategoryGroup(groupId, 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("CategoryGroup", error.getContextAttributes().get("entity-type"));
            assertEquals(groupId.toString(), error.getContextAttributes().get("entity-id"));
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
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        // and: the user creates accounts - and the first account is selected
        UUID accountId = createAccounts(adminAuthTokens, user).get(0);

        CategoryApi categoryApi = new CategoryApi(user.getAuthTokens());

        // and: the user creates a category group
        UUID groupId = categoryApi.createCategoryGroup(new CategoryGroupRequest()
            .name(randomAlphanumeric(20)));
        assertNotNull(groupId);

        // and: the user creates a category within the group
        UUID categoryId = categoryApi.createCategory(groupId, new CategoryRequest()
            .name(randomAlphanumeric(20)));
        assertNotNull(categoryId);

        // when: the user creates category selectors
        List<AccountCategorySelector> setSelectorsRequest = IntStream.range(0, 10)
            .mapToObj(i -> new AccountCategorySelector()
                .creditorContains(randomAlphanumeric(10))
                .refContains(randomAlphanumeric(10))
                .infoContains(randomAlphanumeric(10))
            ).toList();
        List<AccountCategorySelector> selectors = categoryApi.setAccountCategorySelectors(categoryId, accountId, setSelectorsRequest);

        // then: the new selectors are returned
        compare(setSelectorsRequest, selectors);

        // when: the selectors are retrieved
        selectors = categoryApi.getAccountCategorySelectors(categoryId, accountId);

        // then: the selectors are returned
        compare(setSelectorsRequest, selectors);

        // when: the selectors are modified
        // take first 5 original and add 10 new
        List<AccountCategorySelector> updateSelectorsRequest = Streams.concat(
                setSelectorsRequest.stream().limit(5),
                IntStream.range(0, 10).mapToObj(i -> new AccountCategorySelector()
                    .creditorContains(randomAlphanumeric(10))
                    .refContains(randomAlphanumeric(10))
                    .infoContains(randomAlphanumeric(10))
                )
            )
            .toList();

        selectors = categoryApi.setAccountCategorySelectors(categoryId, accountId, updateSelectorsRequest);

        // then: the updated selectors are returned
        compare(updateSelectorsRequest, selectors);

        // when: the updated selectors are retrieved
        selectors = categoryApi.getAccountCategorySelectors(categoryId, accountId);

        // then: the updated selectors are returned
        compare(updateSelectorsRequest, selectors);

        // when: the category is deleted
        categoryApi.deleteCategory(categoryId);

        // then: the category can no longer be retrieved
        withServiceError(categoryApi.getCategory(categoryId, 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("Category", error.getContextAttributes().get("entity-type"));
            assertEquals(categoryId.toString(), error.getContextAttributes().get("entity-id"));
        });

        // and: the selectors can no longer be retrieved
        withServiceError(categoryApi.getAccountCategorySelectors(categoryId, accountId, 404), errorResponse -> {
            ServiceError error = errorResponse.getErrors().get(0);

            // then: a not-found error is returned
            assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
            assertNotNull(error.getContextAttributes());
            assertEquals("Category", error.getContextAttributes().get("entity-type"));
            assertEquals(categoryId.toString(), error.getContextAttributes().get("entity-id"));
        });
    }

    private void compare(Collection<AccountCategorySelector> expected,
                         Collection<AccountCategorySelector> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        expected.forEach(request ->
            assertTrue(actual.stream().anyMatch(selector ->
                request.getCreditorContains().equals(selector.getCreditorContains()) &&
                    request.getRefContains().equals(selector.getRefContains()) &&
                    request.getInfoContains().equals(selector.getInfoContains())
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
        UserConsentRequest consentRequest = new UserConsentRequest()
            .callbackUri(URI.create("http://mock/callback/uri"));
        userConsentApi.register(institution.getId(), consentRequest);

        // and: a user-consent record is waiting to be given
        UserConsentResponse userConsent = userConsentApi.getConsentForInstitution(institution.getId());
        assertEquals("INITIATED", userConsent.getStatus());

        // and: an end-user agreement is created for the institution
        PaginatedList<EndUserAgreement> agreements = agreementAdminApi.list(0, 100);
        assertNotNull(agreements);
        assertEquals(1, agreements.count); // only one as we clear data on each test
        EndUserAgreement agreement = agreements.results.get(0);

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

        try (SendWithBlueSimulator emailSim = new SendWithBlueSimulator(getWiremockPort())) {
            // when: the success response is returned from the rails service
            Response response = userConsentApi.consentResponse(institution.getProvider(), requisition.reference, null, null);

            // then: the redirect response is the original callback URI
            assertEquals(consentRequest.getCallbackUri().toString(), response.getHeader("Location"));

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
