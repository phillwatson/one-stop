package com.hillayes.rail.scheduled;

import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.repository.UserConsentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PollAllConsentsScheduledTaskTest {
    private final UserConsentRepository userConsentRepository = mock();
    private final PollConsentAdhocTask pollConsentAdhocTask = mock();

    @InjectMocks
    private final PollAllConsentsScheduledTask fixture = new PollAllConsentsScheduledTask(
        userConsentRepository,
        pollConsentAdhocTask
    );

    @Test
    public void testGetName() {
        assertEquals("poll-all-consents", fixture.getName());
    }

    @Test
    public void testRun_WithConsents() {
        // given: a collection of accounts
        List<UserConsent> consents = List.of(
            UserConsent.builder().id(UUID.randomUUID()).status(ConsentStatus.GIVEN).build(),
            UserConsent.builder().id(UUID.randomUUID()).status(ConsentStatus.EXPIRED).build(),
            UserConsent.builder().id(UUID.randomUUID()).status(ConsentStatus.GIVEN).build(),
            UserConsent.builder().id(UUID.randomUUID()).status(ConsentStatus.SUSPENDED).build(),
            UserConsent.builder().id(UUID.randomUUID()).status(ConsentStatus.GIVEN).build(),
            UserConsent.builder().id(UUID.randomUUID()).status(ConsentStatus.CANCELLED).build()
        );

        // and: the repository returns the consents
        when(userConsentRepository.listAll()).thenReturn(consents);

        // when: the fixture is invoked
        fixture.run();

        // then: a poll-consent task is queued for each GIVEN consent
        consents.forEach(consent -> {
            if (consent.getStatus() == ConsentStatus.GIVEN) {
                verify(pollConsentAdhocTask).queueTask(consent.getId());
            }
            else {
                verify(pollConsentAdhocTask, never()).queueTask(consent.getId());
            }
        });
    }

    @Test
    public void testRun_WithNoConsents() {
        // given: an empty collection of consents
        List<UserConsent> consents = List.of();

        // and: the repository returns the empty list
        when(userConsentRepository.listAll()).thenReturn(consents);

        // when: the fixture is invoked
        fixture.run();

        // then: NO poll-consent task is queued for any consent
        verify(pollConsentAdhocTask, never()).queueTask(any());
    }
}
