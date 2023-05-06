package com.hillayes.rail.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.ConsentGiven;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.model.AccountSummary;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.scheduled.PollAccountJobbingTask;
import com.hillayes.rail.service.RailAccountService;
import com.hillayes.rail.service.RequisitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.rail.utils.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConsentConsumerTest {
    private UserConsentRepository userConsentRepository;
    private AccountRepository accountRepository;
    private RequisitionService requisitionService;
    private RailAccountService railAccountService;
    private PollAccountJobbingTask pollAccountJobbingTask;

    private ConsentTopicConsumer fixture;

    @BeforeEach
    public void init() {
        userConsentRepository = mock();
        requisitionService = mock();
        railAccountService = mock();
        pollAccountJobbingTask = mock();

        // mock the account save method
        accountRepository = mock();
        when(accountRepository.save(any())).then(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(UUID.randomUUID());
            return account;
        });

        fixture = new ConsentTopicConsumer(
            userConsentRepository,
            accountRepository,
            requisitionService,
            railAccountService,
            pollAccountJobbingTask);
    }

    @Test
    public void testHappyPath() {
        // given: a UserConsent record
        UserConsent userConsent = mockUserConsent(UUID.randomUUID(), ConsentStatus.GIVEN);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: rail accounts to which consent is given
        AccountSummary[] accountSummaries = {
            mockAccountSummary(userConsent.getInstitutionId()),
            mockAccountSummary(userConsent.getInstitutionId())
        };
        when(railAccountService.get(any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Arrays.stream(accountSummaries)
                .filter(acc -> id.equals(acc.id))
                .findFirst();
        });

        // and: a requisition record with rail account IDs
        Requisition requisition = mockRequisition(userConsent, accountSummaries);
        when(requisitionService.get(any())).thenReturn(Optional.of(requisition));

        // and: no existing account records
        when(accountRepository.findByRailAccountId(any())).thenReturn(Optional.empty());

        // and: a ConsentGiven event
        EventPacket eventPacket = spy(mockEventPacket(userConsent));

        // when: the event packet is passed to the consumer
        fixture.consume(eventPacket);

        // then: the event is parsed
        verify(eventPacket).getPayloadContent();

        // and: the user consent is located
        verify(userConsentRepository).findById(userConsent.getId());

        // and: the requisition is retrieved
        verify(requisitionService).get(userConsent.getRequisitionId());

        // and: for each rail account
        for (AccountSummary accountSummary : accountSummaries) {
            // and: the account records are looked up by their rail account id
            verify(accountRepository).findByRailAccountId(accountSummary.id);

            // and: the rail account summaries are retrieved
            verify(railAccountService).get(accountSummary.id);

            // and: the rail account details are retrieved
            verify(railAccountService).details(accountSummary.id);
        }

        // and: each new account record is saved
        verify(accountRepository, times(accountSummaries.length)).save(any());

        // and: each account is scheduled to be polled
        verify(pollAccountJobbingTask, times(accountSummaries.length)).queueJob(any());
    }

    @Test
    public void testUserConsentNotFound() {
        // given: a UserConsent record
        UserConsent userConsent = mockUserConsent(UUID.randomUUID(), ConsentStatus.GIVEN);

        // and: a ConsentGiven event
        EventPacket eventPacket = spy(mockEventPacket(userConsent));

        // and: the UserConsent record cannot be found
        when(userConsentRepository.findById(any())).thenReturn(Optional.empty());

        // when: the event packet is passed to the consumer
        fixture.consume(eventPacket);

        // then: the event is parsed
        verify(eventPacket).getPayloadContent();

        // and: the requisition is retrieved
        verify(requisitionService, never()).get(any());

        // and: the account records are NOT looked up
        verify(accountRepository, never()).findByRailAccountId(any());

        // and: the rail account summaries are NOT retrieved
        verify(railAccountService, never()).get(any());

        // and: the rail account details are NOT retrieved
        verify(railAccountService, never()).details(any());

        // and: NO account record is saved
        verify(accountRepository, never()).save(any());

        // and: NO account is scheduled to be polled
        verify(pollAccountJobbingTask, never()).queueJob(any());
    }

    @Test
    public void testUserConsentNoLongerActive() {
        // given: a UserConsent record
        UserConsent userConsent = mockUserConsent(UUID.randomUUID(), ConsentStatus.GIVEN);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: a ConsentGiven event
        EventPacket eventPacket = spy(mockEventPacket(userConsent));

        // and: the UserConsent has subsequently been cancelled
        userConsent.setStatus(ConsentStatus.CANCELLED);

        // when: the event packet is passed to the consumer
        fixture.consume(eventPacket);

        // then: the event is parsed
        verify(eventPacket).getPayloadContent();

        // and: the requisition is retrieved
        verify(requisitionService, never()).get(any());

        // and: the account records are NOT looked up
        verify(accountRepository, never()).findByRailAccountId(any());

        // and: the rail account summaries are NOT retrieved
        verify(railAccountService, never()).get(any());

        // and: the rail account details are NOT retrieved
        verify(railAccountService, never()).details(any());

        // and: NO account record is saved
        verify(accountRepository, never()).save(any());

        // and: NO account is scheduled to be polled
        verify(pollAccountJobbingTask, never()).queueJob(any());
    }

    private EventPacket mockEventPacket(UserConsent userConsent) {
        ConsentGiven consentGiven = ConsentGiven.builder()
            .dateGiven(userConsent.getDateGiven())
            .userId(userConsent.getUserId())
            .consentId(userConsent.getId())
            .institutionId(userConsent.getInstitutionId())
            .agreementId(userConsent.getAgreementId())
            .agreementExpires(userConsent.getAgreementExpires())
            .build();

        // and: the event is marshalled for delivery
        EventEntity eventEntity = EventEntity.forInitialDelivery(Topic.CONSENT, null, consentGiven);
        return eventEntity.toEventPacket();
    }
}
