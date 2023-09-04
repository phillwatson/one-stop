package com.hillayes.integration.test.rail;

import com.hillayes.integration.api.InstitutionApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.PaginatedInstitutions;
import com.hillayes.sim.nordigen.NordigenSimClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InstitutionTestIT extends ApiTestBase {
    private static NordigenSimClient railClient;

    @BeforeAll
    public static void initRailSim() {
        railClient = newRailClient();
    }

    @BeforeEach
    public void beforeEach() {
        railClient.reset();
    }

    @Test
    public void testListInstitutions() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        // when: the institutions are listed
        InstitutionApi institutionApi = new InstitutionApi(user.getAuthTokens());
        int pageSize = 10;
        PaginatedInstitutions institutions = institutionApi.getInstitutions(1, pageSize, "GB");

        // then: the result contains the institutions
        assertNotNull(institutions);
        assertEquals(1, institutions.getPage());
        assertEquals(pageSize, institutions.getPageSize());
        assertEquals(pageSize, institutions.getCount());
        assertNotNull(institutions.getItems());
        assertEquals(pageSize, institutions.getItems().size());

        long totalItems = institutions.getTotal();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        for (int page = 0; page < totalPages; page++) {
            // when: all pages are retrieved
            institutions = institutionApi.getInstitutions(page, pageSize, "GB");

            // then: the page index and size are returned
            assertNotNull(institutions);
            assertEquals(page, institutions.getPage());
            assertEquals(pageSize, institutions.getPageSize());

            // and: the number of items is correct for the page
            int expectedCount = (totalPages - page > 1) ? pageSize : (int) totalItems - (page * pageSize);
            assertEquals(expectedCount, institutions.getCount());
            assertNotNull(institutions.getItems());
            assertEquals(expectedCount, institutions.getItems().size());

            // and: each entry has an ID and name
            institutions.getItems().forEach(institution -> {
                assertNotNull(institution.getId());
                assertNotNull(institution.getName());
            });
        }
    }

    @Test
    public void testGetInstitution() {
        // given: a user
        UserEntity user = UserEntity.builder()
            .username(randomAlphanumeric(20))
            .givenName(randomAlphanumeric(10))
            .password(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        user = UserUtils.createUser(getWiremockPort(), user);

        // when: the user asks for an institution by ID
        InstitutionApi institutionApi = new InstitutionApi(user.getAuthTokens());
        InstitutionResponse firstDirect = institutionApi.getInstitution("FIRST_DIRECT_MIDLGB22");

        // then: the institution details are returned
        assertNotNull(firstDirect);
        assertEquals("FIRST_DIRECT_MIDLGB22", firstDirect.getId());
        assertEquals("First Direct", firstDirect.getName());
    }
}
