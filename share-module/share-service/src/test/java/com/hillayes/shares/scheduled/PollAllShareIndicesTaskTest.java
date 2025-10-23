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

public class PollAllShareIndicesTaskTest {
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask = mock();

    private final PollAllShareIndicesTask fixture = new PollAllShareIndicesTask(
        shareIndexRepository,
        pollShareIndexAdhocTask
    );

    @Test
    public void testName() {
        assertEquals("poll-all-share-indices", fixture.getName());
    }

    @Test
    public void testRun() {
        // Given: a collection of Share Indices exist
        List<ShareIndex> indices = IntStream.range(0, 10).mapToObj(i ->
            mockShareIndex(b -> b.id(UUID.randomUUID()))
        ).toList();
        when(shareIndexRepository.listAll()).thenReturn(indices);

        // When: the task is run
        fixture.run();

        // Then: the indices are retrieved
        verify(shareIndexRepository).listAll();

        // And: each index is passed to the PollShareIndex task
        verify(pollShareIndexAdhocTask, times(indices.size())).queueTask(any(UUID.class));
        indices.forEach(index ->
            verify(pollShareIndexAdhocTask).queueTask(index.getId())
        );
    }

    @Test
    public void testNoShareIndices() {
        // Given: NO Share Indices exist
        when(shareIndexRepository.listAll()).thenReturn(List.of());

        // When: the task is run
        fixture.run();

        // Then: the indices are retrieved
        verify(shareIndexRepository).listAll();

        // And: NO PollShareIndex task are queued
        verifyNoInteractions(pollShareIndexAdhocTask);
    }
}
