package com.hillayes.rail.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.rail.service.CategoryService;
import com.hillayes.rail.service.UserConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserTopicConsumerTest {
    private UserConsentService userConsentService;
    private CategoryService categoryService;

    private UserTopicConsumer fixture;

    @BeforeEach
    public void init() {
        userConsentService = mock();
        categoryService = mock();

        fixture = new UserTopicConsumer(userConsentService, categoryService);
    }


    @Test
    public void test() {
        // given: a user-deleted event payload
        UserDeleted userDeleted = UserDeleted.builder()
            .dateDeleted(Instant.now())
            .userId(UUID.randomUUID())
            .build();

        // and: the event payment is wrapped in an event packet
        EventPacket eventPacket = new EventPacket(
            UUID.randomUUID(),
            Topic.USER,
            UUID.randomUUID().toString(),
            0, Instant.now(),
            null, userDeleted.getClass().getName(), EventPacket.serialize(userDeleted)
        );

        // when: the event is consumed
        fixture.consume(eventPacket);

        // then: the consents for the deleted user are deleted
        verify(userConsentService).deleteAllConsents(userDeleted.getUserId());

        // and: the categories for the deleted user are deleted
        verify(categoryService).deleteAllCategories(userDeleted.getUserId());
    }
}
