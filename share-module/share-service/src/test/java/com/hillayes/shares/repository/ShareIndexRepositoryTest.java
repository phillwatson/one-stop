package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.ShareIndex;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
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

    @Test
    public void testFindByIsin() {
        // Given: a collection of shares
        List<ShareIndex> indexes = IntStream.range(0, 5)
            .mapToObj(index -> mockShareIndex()).toList();
        shareIndexRepository.saveAll(indexes);

        // When:
        indexes.forEach(expected -> {
            ShareIndex actual = shareIndexRepository.findByIsin(expected.getIsin()).orElse(null);

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
        List<ShareIndex> indexes = IntStream.range(0, 35)
            .mapToObj(index -> mockShareIndex()).toList();
        shareIndexRepository.saveAll(indexes);

        int pageIndex = 0;
        int pageSize = 8;
        int totalPages = (int) Math.ceil((double) indexes.size() / (double) pageSize);
        while (pageIndex < totalPages) {
            // When: each page is requested
            Page<ShareIndex> page = shareIndexRepository.listAll(pageIndex, pageSize);

            // Then: a sub-set of shares are returned
            int expectedSize = (pageIndex < totalPages - 1) ? pageSize : (indexes.size() % pageSize);

            assertNotNull(page);
            assertFalse(page.isEmpty());
            assertEquals(indexes.size(), page.getTotalCount());
            assertEquals(expectedSize, page.getContentSize());
            assertEquals(pageIndex, page.getPageIndex());
            assertEquals(pageSize, page.getPageSize());
            assertEquals(totalPages, page.getTotalPages());

            pageIndex++;
        }
    }
}
