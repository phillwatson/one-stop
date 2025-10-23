package com.hillayes.shares.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.shares.service.SharePriceService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PollShareIndexAdhocTaskTest {
    private final SharePriceService sharePriceService = mock();

    private final PollShareIndexAdhocTask pollShareIndexAdhocTask = new PollShareIndexAdhocTask(
        sharePriceService
    );

    @Test
    public void testName() {
        assertEquals("poll-share-index", pollShareIndexAdhocTask.getName());
    }

    @Test
    public void testWithShareIndexId() {
        // Given: a payload carrying the ID of a Share Index
        UUID shareIndexId = UUID.randomUUID();
        TaskContext<UUID> context = new TaskContext<>(shareIndexId);

        // When: the task is run
        TaskConclusion conclusion = pollShareIndexAdhocTask.apply(context);

        // Then: the Share Index service is called to refresh the share prices
        verify(sharePriceService).refreshSharePrices(shareIndexId);

        // And: the conclusion is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, conclusion);
    }

    @Test
    public void testMissingShareIndexId() {
        // Given: a payload carrying the ID of a Share Index
        TaskContext<UUID> context = new TaskContext<>(null);

        // When: the task is run
        TaskConclusion conclusion = pollShareIndexAdhocTask.apply(context);

        // Then: the Share Index service is NOT called to refresh the share prices
        verifyNoInteractions(sharePriceService);

        // And: the conclusion is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, conclusion);
    }
}
