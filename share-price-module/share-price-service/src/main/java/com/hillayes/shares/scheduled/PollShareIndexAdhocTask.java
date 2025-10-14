package com.hillayes.shares.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedAdhocTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.shares.service.ShareIndexService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollShareIndexAdhocTask extends AbstractNamedAdhocTask<UUID> {
    private final ShareIndexService shareIndexService;

    @Override
    public String getName() {
        return "poll-share-index";
    }

    /**
     * @param context the context containing the identifier of the ShareIndex to be updated.
     */
    @Override
    public TaskConclusion apply(TaskContext<UUID> context) {
        UUID shareIndexId = context.getPayload();
        log.info("Polling Share Index task [shareIndexId: {}]", shareIndexId);

        shareIndexService.refreshSharePrices(shareIndexId);
        return TaskConclusion.COMPLETE;
    }
}
