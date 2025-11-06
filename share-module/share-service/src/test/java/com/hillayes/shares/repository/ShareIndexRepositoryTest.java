package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.ShareIndex;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class ShareIndexRepositoryTest {
    private final ShareIndexRepository shareIndexRepository;

    @BeforeEach
    public void beforeEach() {
        shareIndexRepository.deleteAll();
    }

    @Test
    public void testFindByIsin() {
        // Given: a collection of shares
        List<ShareIndex> indices = IntStream.range(0, 5)
            .mapToObj(index -> mockShareIndex()).toList();
        shareIndexRepository.saveAll(indices);

        // When:
        indices.forEach(expected -> {
            ShareIndex.ShareIdentity shareIdentity = ShareIndex.ShareIdentity.builder()
                .isin(expected.getIdentity().getIsin())
                .tickerSymbol(expected.getIdentity().getTickerSymbol())
                .build();
            ShareIndex actual = shareIndexRepository.findByIdentity(shareIdentity)
                .orElse(null);

            // Then:
            assertNotNull(actual);

            // And:
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getCurrency(), actual.getCurrency());
            assertEquals(expected.getProvider(), actual.getProvider());
        });
    }

    @Test
    public void testListAll() {
        // Given: a collection of shares
        List<ShareIndex> indices = IntStream.range(0, 35)
            .mapToObj(index -> mockShareIndex()).toList();
        shareIndexRepository.saveAll(indices);

        int pageIndex = 0;
        int pageSize = 8;
        int totalPages = (int) Math.ceil((double) indices.size() / (double) pageSize);
        while (pageIndex < totalPages) {
            // When: each page is requested
            Page<ShareIndex> page = shareIndexRepository.listAll(pageIndex, pageSize);

            // Then: a sub-set of shares are returned
            int expectedSize = (pageIndex < totalPages - 1) ? pageSize : (indices.size() % pageSize);

            assertNotNull(page);
            assertFalse(page.isEmpty());
            assertEquals(indices.size(), page.getTotalCount());
            assertEquals(expectedSize, page.getContentSize());
            assertEquals(pageIndex, page.getPageIndex());
            assertEquals(pageSize, page.getPageSize());
            assertEquals(totalPages, page.getTotalPages());

            pageIndex++;
        }
    }

    @Test
    public void testDuplicateShareIndex() {
        // Given: an existing share index
        ShareIndex existingIndex = shareIndexRepository.saveAndFlush(mockShareIndex());

        // When: another share index with the same ISIN is saved
        ConstraintViolationException expected = assertThrows(ConstraintViolationException.class, () ->
            shareIndexRepository.saveAndFlush(
                mockShareIndex(s ->
                    s.identity(ShareIndex.ShareIdentity.builder()
                        .isin(existingIndex.getIdentity().getIsin())
                        .tickerSymbol(existingIndex.getIdentity().getTickerSymbol())
                        .build())
                )
            )
        );

        // Then: the exception is due to a unique constraint violation
        assertEquals(ConstraintViolationException.ConstraintKind.UNIQUE, expected.getKind());
        assertEquals("share_index_isin_key", expected.getConstraintName());
    }
}
