package com.hillayes.shares.scheduled;

import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.repository.ShareIndexRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PollAllShareIndexesTaskTest {
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask = mock();

    private final PollAllShareIndexesTask fixture = new PollAllShareIndexesTask(
        shareIndexRepository,
        pollShareIndexAdhocTask
    );

    @Test
    public void testName() {
        assertEquals("poll-all-share-indexes", fixture.getName());
    }

    @Test
    public void testRun() {
        // Given: a collection of Share Indexes exist
        List<ShareIndex> indexes = IntStream.range(0, 10).mapToObj(i ->
            mockShareIndex(b -> b.id(UUID.randomUUID()))
        ).toList();
        when(shareIndexRepository.listAll()).thenReturn(indexes);

        // When: the task is run
        fixture.run();

        // Then: the indexes are retrieved
        verify(shareIndexRepository).listAll();

        // And: each index is passed to the PollShareIndex task
        verify(pollShareIndexAdhocTask, times(indexes.size())).queueTask(any(UUID.class));
        indexes.forEach(index ->
            verify(pollShareIndexAdhocTask).queueTask(index.getId())
        );
    }

    @Test
    public void testNoShareIndexes() {
        // Given: NO Share Indexes exist
        when(shareIndexRepository.listAll()).thenReturn(List.of());

        // When: the task is run
        fixture.run();

        // Then: the indexes are retrieved
        verify(shareIndexRepository).listAll();

        // And: NO PollShareIndex task are queued
        verifyNoInteractions(pollShareIndexAdhocTask);
    }
}
