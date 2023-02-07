package com.hillayes.user.service;

import com.hillayes.user.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class UserServiceTest {
    @Inject
    UserService service;

    @Test
    public void testCreateUser() {
        service.createUser("username", "password".toCharArray(),
            User.builder()
                .givenName("test")
                .familyName("user")
                .email("email")
                .phoneNumber("123456")
                .build());
    }
}
