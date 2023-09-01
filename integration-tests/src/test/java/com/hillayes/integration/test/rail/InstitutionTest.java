package com.hillayes.integration.test.rail;

import com.hillayes.integration.api.AuthApi;
import com.hillayes.integration.api.InstitutionApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.onestop.api.PaginatedInstitutions;
import com.hillayes.sim.nordigen.NordigenSimClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InstitutionTest extends ApiTestBase {
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
    public void test() {
        // given: an admin has signed in
        AuthApi authApi = new AuthApi();
        Map<String, String> authTokens = authApi.login("admin", "password");
        assertNotNull(authTokens);
        assertEquals(3, authTokens.size());

        // when: the institutions are listed
        InstitutionApi institutionApi = new InstitutionApi(authTokens);
        PaginatedInstitutions institutions = institutionApi.getInstitutions(1, 10, "GB");

        // then: the result contains the institutions
        assertNotNull(institutions);
        assertEquals(1, institutions.getPage());
        assertEquals(10, institutions.getPageSize());
        assertEquals(10, institutions.getItems().size());
    }
}
