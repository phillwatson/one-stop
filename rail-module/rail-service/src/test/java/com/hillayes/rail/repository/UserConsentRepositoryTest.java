package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestTransaction
public class UserConsentRepositoryTest {
    @Inject
    UserConsentRepository fixture;

    @Test
    public void testFindByUserId() {
        // given: a collection of user IDs
        Collection<UUID> userIds = List.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // and: three user-consents for each of them
        userIds.forEach(userId -> {
            for (int i = 0; i < 3; i++) {
                fixture.save(UserConsent.builder()
                    .provider(RailProvider.NORDIGEN)
                    .reference(UUID.randomUUID().toString())
                    .userId(userId)
                    .institutionId(UUID.randomUUID().toString())
                    .agreementId(UUID.randomUUID().toString())
                    .agreementExpires(Instant.now().plusSeconds(1000))
                    .maxHistory(80)
                    .status(ConsentStatus.GIVEN)
                    .build());
            }
        });

        userIds.forEach(userId -> {
            // when: each user-id retrieves their consents
            List<UserConsent> consents = fixture.findByUserId(userId);

            // then: the count is correct
            assertEquals(3, consents.size());

            // and: each belongs to the identified user
            consents.forEach(consent -> assertEquals(userId, consent.getUserId()));
        });
    }

    @Test
    public void testFindByUserId_Paged() {
        // given: a collection of user IDs
        Collection<UUID> userIds = List.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // and: three user-consents for each of them
        userIds.forEach(userId -> {
            for (int i = 0; i < 35; i++) {
                fixture.save(UserConsent.builder()
                    .provider(RailProvider.NORDIGEN)
                    .reference(UUID.randomUUID().toString())
                    .userId(userId)
                    .institutionId(UUID.randomUUID().toString())
                    .agreementId(UUID.randomUUID().toString())
                    .agreementExpires(Instant.now().plusSeconds(1000))
                    .maxHistory(80)
                    .status(ConsentStatus.GIVEN)
                    .build());
            }
        });

        userIds.forEach(userId -> {
            // when: each user-id retrieves their consents
            for (int page = 0; page < 4; page++) {
                Page<UserConsent> consents = fixture.findByUserId(userId, page, 10);

                // then: the page number is correct
                assertEquals(page, consents.getPageIndex());

                // and: the totals are correct
                assertEquals(35, consents.getTotalCount());
                assertEquals(4, consents.getTotalPages());

                // and: the count is correct
                assertEquals(page == 3 ? 5 : 10, consents.getContentSize());

                // and: each belongs to the identified user
                consents.forEach(consent -> assertEquals(userId, consent.getUserId()));
            }
        });
    }
}
