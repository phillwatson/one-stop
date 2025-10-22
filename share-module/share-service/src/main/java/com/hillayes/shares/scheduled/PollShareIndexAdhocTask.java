package com.hillayes.shares.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedAdhocTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.shares.service.SharePriceService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * An adhoc task to retrieve the latest share prices for the ShareIndex identified
 * in the payload.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollShareIndexAdhocTask extends AbstractNamedAdhocTask<UUID> {
    private final SharePriceService sharePriceService;

    @Override
    public String getName() {
        return "poll-share-index";
    }

    /**
     * Performs the task.
     *
     * @param context the context containing the identifier of the ShareIndex to be updated.
     */
    @Override
    public TaskConclusion apply(TaskContext<UUID> context) {
        UUID shareIndexId = context.getPayload();
        log.info("Polling Share Index task [shareIndexId: {}]", shareIndexId);

        if (shareIndexId != null) {
            sharePriceService.refreshSharePrices(shareIndexId);
        }
        return TaskConclusion.COMPLETE;
    }
}
