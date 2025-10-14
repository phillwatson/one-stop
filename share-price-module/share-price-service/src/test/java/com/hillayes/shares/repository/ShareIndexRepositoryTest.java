package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.ShareIndex;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class ShareIndexRepositoryTest {
    private final ShareIndexRepository shareIndexRepository;

    @Test
    public void testFindByIsin() {
        // Given:
        List<ShareIndex> indexes = List.of(
            mockShareIndex(null),
            mockShareIndex(null),
            mockShareIndex(null),
            mockShareIndex(null),
            mockShareIndex(null)
        );

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
        // When:
        Page<ShareIndex> page = shareIndexRepository.listAll(2, 20);

        // Then:
        assertNotNull(page);
        assertTrue(page.isEmpty());
        assertEquals(0, page.getContentSize());
        assertEquals(2, page.getPageIndex());
        assertEquals(20, page.getPageSize());
        assertEquals(0, page.getTotalPages());
    }
}
