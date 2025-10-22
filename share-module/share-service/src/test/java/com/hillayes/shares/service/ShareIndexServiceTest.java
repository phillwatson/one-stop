package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.utils.TestData;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShareIndexServiceTest {
    private final ShareIndexRepository shareIndexRepository = mock();

    private final ShareIndexService fixture = new ShareIndexService(
        shareIndexRepository
    );

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

        // And: the repository is able to save the record
        when(shareIndexRepository.save(any(ShareIndex.class)))
            .then(invovation -> invovation.getArgument(0));

        // When: the service is called
        ShareIndex shareIndex = fixture.registerShareIndex(isin, name, currency, provider);

        // Then: a new share index is returned
        assertNotNull(shareIndex);

        // And: the record matches the given data
        assertEquals(isin, shareIndex.getIsin());
        assertEquals(name, shareIndex.getName());
        assertEquals(currency, shareIndex.getCurrency());
        assertEquals(provider, shareIndex.getProvider());
    }

    @Test
    public void testListShareIndexes() {
        // Given: a collection of ShareIndexes exists
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
        Page<ShareIndex> page = fixture.listShareIndexes(1, 20);

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
