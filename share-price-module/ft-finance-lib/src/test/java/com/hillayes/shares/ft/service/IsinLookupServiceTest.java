package com.hillayes.shares.ft.service;

import com.hillayes.shares.ft.client.MarketsClient;
import com.hillayes.shares.ft.domain.IsinIssueLookup;
import com.hillayes.shares.ft.repository.IsinIssueLookupRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IsinLookupServiceTest {
    private static IsinIssueLookupRepository isinIssueLookupRepository;
    private static MarketsClient marketsClient;

    private static IsinLookupService isinLookupService;

    @BeforeAll
    public static void beforeAll() {
        isinIssueLookupRepository = mock();
        marketsClient = spy(new MarketsClient());

        isinLookupService = new IsinLookupService(isinIssueLookupRepository, marketsClient);
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(isinIssueLookupRepository);
    }

    @Test
    public void testIsinNotCached() {
        // Given: a valid ISIN
        String stockIsin = "GB00B0CNGT73:GBP";

        // And: the ISIN is not cached locally
        when(isinIssueLookupRepository.findByIsin(stockIsin)).thenReturn(Optional.empty());

        // When: The issue ID is requested
        Optional<String> issueId = isinLookupService.lookupIssueId(stockIsin);

        // Then: the issue ID is retrieved from the market client
        verify(marketsClient).getIssueID(stockIsin);

        // And: the result is as expected
        assertNotNull(issueId);
        assertTrue(issueId.isPresent());
        assertEquals("74137468", issueId.get());

        // And: the retrieved value is persisted to the local cache
        ArgumentCaptor<IsinIssueLookup> lookupCaptor = ArgumentCaptor.forClass(IsinIssueLookup.class);
        verify(isinIssueLookupRepository).save(lookupCaptor.capture());

        // And: the record should hold both the ISIN and issueId
        IsinIssueLookup value = lookupCaptor.getValue();
        assertEquals(stockIsin, value.getIsin());
        assertEquals(issueId.get(), value.getIssueId());
    }

    @Test
    public void testIsinIsCached() {
        // Given: a valid ISIN
        String stockIsin = "GB00B0CNGT73:GBP";

        // And: the ISIN is cached locally
        when(isinIssueLookupRepository.findByIsin(stockIsin)).thenReturn(
            Optional.of(IsinIssueLookup.builder()
                .isin(stockIsin)
                .issueId("74137468")
                .build())
        );

        // When: The issue ID is requested
        Optional<String> issueId = isinLookupService.lookupIssueId(stockIsin);

        // Then: the issue ID is NOT retrieved from the market client
        verify(marketsClient, never()).getIssueID(stockIsin);

        // And: the result is as expected
        assertNotNull(issueId);
        assertTrue(issueId.isPresent());
        assertEquals("74137468", issueId.get());
    }
}
