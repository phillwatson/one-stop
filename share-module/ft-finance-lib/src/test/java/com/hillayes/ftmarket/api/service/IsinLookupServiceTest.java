package com.hillayes.ftmarket.api.service;

import com.hillayes.ftmarket.api.client.MarketsClient;
import com.hillayes.ftmarket.api.domain.IsinIssueLookup;
import com.hillayes.ftmarket.api.repository.IsinIssueLookupRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class IsinLookupServiceTest {
    @InjectMock
    IsinIssueLookupRepository isinIssueLookupRepository;

    @InjectSpy
    MarketsClient marketsClient;

    @Inject
    IsinLookupService isinLookupService;

    @BeforeEach
    public void beforeEach() {
        // mock default behaviour
        when(isinIssueLookupRepository.save(any()))
            .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @ParameterizedTest
    @MethodSource("symbolSource")
    public void testIsinNotCached(String symbol, String expectedIssueId, String expectedName) {
        // Given: the symbol is not cached locally
        when(isinIssueLookupRepository.findByIsin(symbol)).thenReturn(Optional.empty());

        // When: The issue ID is requested
        Optional<IsinIssueLookup> issueId = isinLookupService.lookupIssueId(symbol);

        // Then: the issue ID is retrieved from the market client
        verify(marketsClient).getIssueID(symbol);

        // And: the result is as expected
        assertNotNull(issueId);
        assertTrue(issueId.isPresent());
        assertEquals(expectedIssueId, issueId.get().getIssueId());
        assertEquals(expectedName, issueId.get().getName());
        assertEquals("GBP", issueId.get().getCurrencyCode());

        // And: the retrieved value is persisted to the local cache
        ArgumentCaptor<IsinIssueLookup> lookupCaptor = ArgumentCaptor.forClass(IsinIssueLookup.class);
        verify(isinIssueLookupRepository).save(lookupCaptor.capture());

        // And: the record should hold both the symbol and issueId
        IsinIssueLookup value = lookupCaptor.getValue();
        assertEquals(symbol, value.getIsin());
        assertEquals(issueId.get().getIssueId(), value.getIssueId());
    }

    @Test
    public void testIsinIsCached() {
        // Given: a valid symbol
        String symbol = "GB00B0CNGT73";

        // And: the symbol is cached locally
        when(isinIssueLookupRepository.findByIsin(symbol)).thenReturn(
            Optional.of(IsinIssueLookup.builder()
                .isin(symbol)
                .issueId("74137468")
                .build())
        );

        // When: The issue ID is requested
        Optional<IsinIssueLookup> issueId = isinLookupService.lookupIssueId(symbol);

        // Then: the issue ID is NOT retrieved from the market client
        verify(marketsClient, never()).getIssueID(symbol);

        // And: the result is as expected
        assertNotNull(issueId);
        assertTrue(issueId.isPresent());
        assertEquals("74137468", issueId.get().getIssueId());
    }

    private static Stream<Arguments> symbolSource() {
        return Stream.of(
            Arguments.of("GB00B8XYYQ86", "52606664", "Royal London Short Term Money Market Fund Y Acc"),
            Arguments.of("TW.", "274273", "Taylor Wimpey PLC"),
            Arguments.of("AV.", "54712", "Aviva PLC")
        );
    }
}
