package com.hillayes.user.utils;

import com.hillayes.user.domain.User;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.insecure;

public class TestData {
    public static List<User> mockUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> mockUser())
            .toList();
    }

    public static User mockUser() {
        return User.builder()
            .username(insecure().nextAlphanumeric(20))
            .preferredName(insecure().nextAlphanumeric(20))
            .title(insecure().nextAlphanumeric(10))
            .givenName(insecure().nextAlphanumeric(20))
            .familyName(insecure().nextAlphanumeric(20))
            .email(insecure().nextAlphanumeric(20))
            .phoneNumber(insecure().nextNumeric(10))
            .passwordHash(UUID.randomUUID().toString())
            .passwordLastSet(Instant.now().minus(Duration.ofDays(10)))
            .locale(Locale.ENGLISH)
            .roles(Set.of("user"))
            .build();
    }

    public static User mockUser(UUID id) {
        User result = mockUser();
        result.setId(id);
        return result;
    }
}
