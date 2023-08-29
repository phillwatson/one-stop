package com.hillayes.integration.test.user;

import com.hillayes.integration.api.AuthApi;
import com.hillayes.integration.api.UserAdminApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.onestop.api.PaginatedUsers;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserAdminTest extends ApiTestBase {

    @Test
    public void testListUsers() {
        // given: the admin user signs in
        AuthApi authApi = new AuthApi();
        Map<String, String> cookies = authApi.login("admin", "password");
        assertNotNull(cookies);
        assertEquals(3, cookies.size());

        // when: the admin retrieves the users
        UserAdminApi userAdminApi = new UserAdminApi(cookies);
        PaginatedUsers users = userAdminApi.listUsers(0, 30);
        assertNotNull(users);

        // then: the results reflects selected page index and size
        assertEquals(0, users.getPage());
        assertEquals(30, users.getPageSize());

        // and: the admin user is in the list
        assertNotNull(users.getItems());
        assertNotNull(users.getItems().stream()
            .filter(user -> "admin".equals(user.getUsername()))
            .findFirst().orElse(null));
    }
}
