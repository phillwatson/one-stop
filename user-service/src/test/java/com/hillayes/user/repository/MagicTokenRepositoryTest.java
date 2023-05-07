package com.hillayes.user.repository;

import com.hillayes.user.domain.MagicToken;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class MagicTokenRepositoryTest {
    @Inject
    MagicTokenRepository fixture;

    private List<MagicToken> entries;

    @Test
    public void testFindByToken() {
        // given: some magic tokens in the database
        entries = IntStream.range(0, 3)
            .mapToObj(i ->
                MagicToken.builder()
                    .token(UUID.randomUUID().toString())
                    .email(RandomStringUtils.randomAlphanumeric(30))
                    .expires(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build()
            ).toList();
        fixture.saveAll(entries);

        // when: we search for each token
        for (MagicToken entry : entries) {
            Optional<MagicToken> found = fixture.findByToken(entry.getToken());

            // then: we should find the token
            assertTrue(found.isPresent());
            assertEquals(entry.getId(), found.get().getId());
            assertEquals(entry.getToken(), found.get().getToken());
            assertEquals(entry.getEmail(), found.get().getEmail());
        }
    }

    @Test
    public void testDeleteByExpiresBefore() {
        // given: an expired entry
        MagicToken expiredEntry = fixture.saveAndFlush(MagicToken.builder()
            .token(UUID.randomUUID().toString())
            .email(RandomStringUtils.randomAlphanumeric(30))
            .expires(Instant.now().minusSeconds(10))
            .build());
        assertTrue(fixture.findByToken(expiredEntry.getToken()).isPresent());

        // and: some non-expired entries
        entries = IntStream.range(0, 3)
            .mapToObj(i ->
                MagicToken.builder()
                    .token(UUID.randomUUID().toString())
                    .email(RandomStringUtils.randomAlphanumeric(30))
                    .expires(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build()
            ).toList();
        fixture.saveAll(entries);

        // when: we delete all expired entries
        long count = fixture.deleteByExpiresBefore(Instant.now());

        // then: only the expired entry should be deleted
        assertEquals(1, count);

        // and: we can't retrieve the expired entry
        assertFalse(fixture.findByToken(expiredEntry.getToken()).isPresent());
    }
}
