package com.hillayes.user.utils;

import com.hillayes.user.domain.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

public class TestData {
    public static List<User> mockUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> mockUser())
            .toList();
    }

    public static User mockUser() {
        return User.builder()
            .username(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .email(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(10))
            .passwordHash(UUID.randomUUID().toString())
            .roles(Set.of("user"))
            .build();
    }

    public static User mockUser(UUID id) {
        return mockUser().toBuilder()
            .id(id)
            .build();
    }
}
