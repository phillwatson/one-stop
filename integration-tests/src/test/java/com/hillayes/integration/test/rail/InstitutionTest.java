package com.hillayes.integration.test.rail;

import com.hillayes.integration.api.AuthApi;
import com.hillayes.integration.api.InstitutionApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.sim.nordigen.NordigenSimulator;
import com.hillayes.onestop.api.PaginatedInstitutions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InstitutionTest extends ApiTestBase {
    // @Test
    public void testGetInstitutions_Admin() {
        try (NordigenSimulator railSim = new NordigenSimulator(getWiremockPort())) {
            // given: the admin user signs in
            AuthApi authApi = new AuthApi();
            Map<String, String> authTokens = authApi.login("admin", "password");
            assertNotNull(authTokens);

            // when: the admin retrieves the list of institutions
            InstitutionApi institutionApi = new InstitutionApi(authTokens);
            PaginatedInstitutions gbInstitutions = institutionApi.getInstitutions(0, 20, "GB");

            // then: the institutions are returned
            assertNotNull(gbInstitutions);
            assertEquals(0, gbInstitutions.getPage());
            assertEquals(20, gbInstitutions.getPageSize());
            assertNotNull(gbInstitutions.getItems());
            assertEquals(gbInstitutions.getCount(), gbInstitutions.getItems().size());
        }
    }
}
