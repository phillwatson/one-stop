package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.portfolio.SharesTransacted;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@TopicConsumer(Topic.PORTFOLIO)
@RequiredArgsConstructor
@Slf4j
public class PortfolioTopicConsumer implements EventConsumer {
    private final SendEmailTask sendEmailTask;

    @Transactional
    public void consume(EventPacket eventPacket) {
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
