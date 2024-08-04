package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class EventDelivererTest {
    @Mock
    @TopicObserved(Topic.USER)
    Event<EventPacket> userEvent;

    @Mock
    @TopicObserved(Topic.USER_AUTH)
    Event<EventPacket> userAuthEvent;

    @Mock
    @TopicObserved(Topic.CONSENT)
    Event<EventPacket> consentEvent;

    @Mock
    @TopicObserved(Topic.HOSPITAL_TOPIC)
    Event<EventPacket> hospitalEvent;

    @Mock
    EventRepository eventRepository;

    @InjectMocks
    EventDeliverer fixture = new EventDeliverer();

    @BeforeEach
    public void beforeEach() {
        openMocks(this);
    }

    @Test
    public void testDeliverEvents_noEvents() {
        // given: no waiting events
        when(eventRepository.listUndelivered(anyInt())).thenReturn(Collections.emptyList());

        // when: events are delivered
        fixture.deliverEvents();

        // then: waiting events are retrieved from the repository
        verify(eventRepository).listUndelivered(anyInt());
    }

    @Test
    public void testDeliverEvents() {
        // given: waiting events
        List<EventEntity> waitingEvents = List.of(
            createEventEntity(Topic.USER_AUTH),
            createEventEntity(Topic.USER_AUTH),
            createEventEntity(Topic.USER_AUTH)
        );
        when(eventRepository.listUndelivered(anyInt())).thenReturn(waitingEvents);

        // when: events are delivered
        fixture.deliverEvents();

        // then: waiting events are retrieved from the repository
        verify(eventRepository).listUndelivered(anyInt());

        // and: events are all sent to the user-auth topic
        verify(userAuthEvent, times(waitingEvents.size())).fire(any());

        // and: no events are sent to the other topics
        verify(userEvent, never()).fire(any());
        verify(consentEvent, never()).fire(any());
        verify(hospitalEvent, never()).fire(any());

        // and: the events are deleted from the repository
        verify(eventRepository, times(waitingEvents.size())).delete(any());
    }

    private EventEntity createEventEntity(Topic topic) {
        // given: an event payload
        UserAuthenticated event = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event entity is created
        return EventEntity.forInitialDelivery(topic, "test-key", event);
    }

    private CompletionStage<EventPacket> mockCompletionStage() {
        CompletionStage<EventPacket> result = mock(CompletionStage.class);
        CompletableFuture<EventPacket> mockFuture = mock(CompletableFuture.class);
        when(result.toCompletableFuture()).thenReturn(mockFuture);

        return result;
    }
}
