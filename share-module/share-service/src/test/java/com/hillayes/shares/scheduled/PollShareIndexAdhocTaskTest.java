package com.hillayes.shares.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.shares.service.ShareIndexService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PollShareIndexAdhocTaskTest {
    private final ShareIndexService shareIndexService = mock();

    private final PollShareIndexAdhocTask pollShareIndexAdhocTask = new PollShareIndexAdhocTask(
        shareIndexService
    );

    @Test
    public void testName() {
        assertEquals("poll-share-index", pollShareIndexAdhocTask.getName());
    }

    @Test
    public void test() {
        // Given: a payload carrying the ID of a Share Index
        UUID shareIndexId = UUID.randomUUID();
        TaskContext<UUID> context = new TaskContext<>(shareIndexId);

        // When: the task is run
        TaskConclusion conclusion = pollShareIndexAdhocTask.apply(context);

        // Then: the Share Index service is called to refresh the share prices
        verify(shareIndexService).refreshSharePrices(shareIndexId);

        // And: the conclusion is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, conclusion);
    }
}
