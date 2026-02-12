package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.ShareInfo;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.scheduled.PollShareIndexAdhocTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.hillayes.shares.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class ShareIndexServiceTest {
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask = mock();
    private final ShareProviderFactory providerFactory = mock();
    private final ShareProviderApi shareProviderApi = mock();

    private final ShareIndexService fixture = new ShareIndexService(
        shareIndexRepository,
        pollShareIndexAdhocTask,
        providerFactory
    );

    @BeforeEach
    public void beforeEach() {
        when(shareProviderApi.getProviderId()).thenReturn(ShareProvider.FT_MARKET_DATA);
        when(providerFactory.getAll()).then( i ->
            Stream.of(shareProviderApi)
        );

        when(shareIndexRepository.saveAndFlush(any(ShareIndex.class)))
            .then(invocation -> {
                ShareIndex entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    // assign a new UUID
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });
    }

    @Test
    public void testGetShareIndex_Found() {
        // Given: a share index exists
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.of(shareIndex));

        // When: the service is called
        Optional<ShareIndex> result = fixture.getShareIndex(shareIndex.getId());

        // Then: the identified index is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertSame(shareIndex, result.get());
    }

    @Test
    public void testGetShareIndex_NotFound() {
        // Given: an unknown share index ID
        UUID shareIndexId = UUID.randomUUID();
        when(shareIndexRepository.findByIdOptional(shareIndexId))
            .thenReturn(Optional.empty());

        // When: the service is called
        Optional<ShareIndex> result = fixture.getShareIndex(shareIndexId);

        // Then: NO index is returned
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void testRegisterShareIndex() {
        // Given: new share index identity
        ShareIndex.ShareIdentity request = mockShareIdentity();

        // And: the share provider will return the share info for the identity
        ShareInfo shareInfo = new ShareInfo(request.getIsin(), request.getTickerSymbol(),
            randomStrings.nextAlphanumeric(30), "GBP");
        when(shareProviderApi.getShareInfo(request.getIsin(), request.getTickerSymbol()))
            .thenReturn(Optional.of(shareInfo));

        // When: the service is called
        ShareIndex shareIndex = fixture.registerShareIndex(request);

        // Then: a new share index is returned
        assertNotNull(shareIndex);

        // And: the record matches the given data
        assertEquals(request.getIsin(), shareIndex.getIdentity().getIsin());
        assertEquals(request.getTickerSymbol(), shareIndex.getIdentity().getTickerSymbol());
        assertEquals(shareInfo.getName(), shareIndex.getName());
        assertEquals(shareInfo.getCurrency(), shareIndex.getCurrency());
        assertEquals(shareProviderApi.getProviderId(), shareIndex.getProvider());

        // And: the repository was called
        verify(shareIndexRepository).saveAndFlush(any(ShareIndex.class));

        // And: the share index polling task was queued
        verify(pollShareIndexAdhocTask).queueTask(shareIndex.getId());
    }

    @Test
    public void testRegisterShareIndices() {
        // Given: a collection of new share identities
        Map<ShareIndex.ShareIdentity, ShareInfo> shareIds = IntStream.range(0, 10)
            .mapToObj(i -> mockShareIdentity())
            .collect(Collectors.toMap(id -> id, id ->
                    new ShareInfo(id.getIsin(), id.getTickerSymbol(),
                        randomStrings.nextAlphanumeric(20), "GBP")));

        // And: the providers will return the share info for those identities
        when(shareProviderApi.getShareInfo(anyString(), anyString())).then(invocation -> {
                String isin = invocation.getArgument(0);
                String ticker = invocation.getArgument(1);
                return shareIds.entrySet().stream()
                .filter(info -> {
                    ShareIndex.ShareIdentity id = info.getKey();
                    return isin.equals(id.getIsin()) && ticker.equals(id.getTickerSymbol());
                })
                .map(Map.Entry::getValue)
                .findFirst();
        });

        // When: the service is called
        Collection<ShareIndex> result = fixture.registerShareIndices(shareIds.keySet());

        // Then: the result is the same size
        assertNotNull(result);
        assertEquals(shareIds.size(), result.size());

        // And: each index has the same properties as given
        shareIds.forEach((expected, info) -> {
            ShareIndex actual = result.stream()
                .filter(s -> s.getIdentity().getIsin().equals(expected.getIsin()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertNotNull(actual.getId());
            assertEquals(expected.getIsin(), actual.getIdentity().getIsin());
            assertEquals(expected.getTickerSymbol(), actual.getIdentity().getTickerSymbol());
            assertEquals(info.getName(), actual.getName());
            assertEquals(info.getCurrency(), actual.getCurrency());
            assertEquals(shareProviderApi.getProviderId(), actual.getProvider());
        });

        // And: the repository was called for each index
        verify(shareIndexRepository, times(shareIds.size())).saveAndFlush(any(ShareIndex.class));

        // And: a task was queued for each index
        verify(pollShareIndexAdhocTask, times(shareIds.size())).queueTask(any(UUID.class));
    }

    @Test
    public void testRegisterShareIndices_EmptyList() {
        // When: the service is called with an empty list
        Collection<ShareIndex> result = fixture.registerShareIndices(List.of());

        // Then: the result is the same size
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // And: the repository is NOT called
        verifyNoInteractions(shareIndexRepository);

        // And: NO task was queued for each index
        verifyNoInteractions(pollShareIndexAdhocTask);
    }

    @Test
    public void testRegisterShareIndices_NullList() {
        // When: the service is called with an null list
        Collection<ShareIndex> result = fixture.registerShareIndices(null);

        // Then: the result is the same size
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // And: the repository is NOT called
        verifyNoInteractions(shareIndexRepository);

        // And: NO task was queued for each index
        verifyNoInteractions(pollShareIndexAdhocTask);
    }

    @Test
    public void testListShareIndices() {
        // Given: a collection of ShareIndices exists
        List<ShareIndex> shares = IntStream.range(0, 100)
            .mapToObj(i -> mockShareIndex(s -> s.id(UUID.randomUUID())))
            .sorted(Comparator.comparing(ShareIndex::getName))
            .toList();

        // And: the repository returns the list as a page
        when(shareIndexRepository.listAll(anyInt(), anyInt())).then(invocation -> {
            int pageIndex = invocation.getArgument(0);
            int pageSize = invocation.getArgument(1);
            return Page.of(shares, pageIndex, pageSize);
        });

        // When: the service is called
        Page<ShareIndex> page = fixture.listShareIndices(1, 20);

        // Then: the requested page is returned
        assertNotNull(page);
        assertFalse(page.isEmpty());
        assertEquals(shares.size(), page.getTotalCount());
        assertEquals(5, page.getTotalPages());
        assertEquals(1, page.getPageIndex());
        assertEquals(20, page.getPageSize());
        assertEquals(20, page.getContentSize());
    }
}
