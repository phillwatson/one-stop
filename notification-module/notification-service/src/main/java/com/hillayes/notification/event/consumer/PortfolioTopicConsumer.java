package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.portfolio.SharesTransacted;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PortfolioTopicConsumer {
    private final SendEmailTask sendEmailTask;

    @TopicObserver
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes
                        @TopicObserved(Topic.PORTFOLIO)EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received portfolio event [payloadClass: {}]", payloadClass);

        Map<String, Object> params = new HashMap<>();
        if (SharesTransacted.class.getName().equals(payloadClass)) {
            SharesTransacted event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmailTask.queueTask(event.getUserId(), TemplateName.SHARES_TRANSACTED, params);
        }
    }
}
