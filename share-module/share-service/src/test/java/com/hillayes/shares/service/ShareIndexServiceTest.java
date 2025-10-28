package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.scheduled.PollShareIndexAdhocTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class ShareIndexServiceTest {
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask = mock();

    private final ShareIndexService fixture = new ShareIndexService(
        shareIndexRepository,
        pollShareIndexAdhocTask
    );

    @BeforeEach
    public void beforeEach() {
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
        // Given: new share index date
        String isin = "GB00B80QG052";
        String name = "HSBC FTSE 250 Index Accumulation C";
        Currency currency = Currency.getInstance("GBP");
        ShareProvider provider = ShareProvider.FT_MARKET_DATA;

        // When: the service is called
        ShareIndex shareIndex = fixture.registerShareIndex(isin, name, currency, provider);

        // Then: a new share index is returned
        assertNotNull(shareIndex);

        // And: the record matches the given data
        assertEquals(isin, shareIndex.getIsin());
        assertEquals(name, shareIndex.getName());
        assertEquals(currency, shareIndex.getCurrency());
        assertEquals(provider, shareIndex.getProvider());

        // And: the repository was called
        verify(shareIndexRepository).saveAndFlush(any(ShareIndex.class));

        // And: the share index polling task was queued
        verify(pollShareIndexAdhocTask).queueTask(shareIndex.getId());
    }

    @Test
    public void testRegisterShareIndices() {
        // Given: a collection of new share indices
        List<ShareIndex> shareIndices = IntStream.range(0, 10)
            .mapToObj(i -> mockShareIndex()).toList();

        // When: the service is called
        Collection<ShareIndex> result = fixture.registerShareIndices(shareIndices);

        // Then: the result is the same size
        assertNotNull(result);
        assertEquals(shareIndices.size(), result.size());

        // And: each index has the same properties as given
        shareIndices.forEach(expected -> {
            ShareIndex actual = result.stream()
                .filter(s -> s.getIsin().equals(expected.getIsin()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertNotNull(actual.getId());
            assertEquals(expected.getIsin(), actual.getIsin());
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getCurrency(), actual.getCurrency());
            assertEquals(expected.getProvider(), actual.getProvider());
        });

        // And: the repository was called for each index
        verify(shareIndexRepository, times(shareIndices.size())).saveAndFlush(any(ShareIndex.class));

        // And: a task was queued for each index
        verify(pollShareIndexAdhocTask, times(shareIndices.size())).queueTask(any(UUID.class));
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
