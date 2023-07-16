
# One-Stop

---
## Events Library
A shared library used by those services wishing to listen for events published
by the message broker (i.e. outbox-lib). Those services that are only interested
in listening for events, and not interested in publishing them, only need
include this library in their dependencies.

Those services wishing to publish events must also include a dependency on the
library `outbox-lib`.

### Topics
Event topics are defined in the enum `com.hillayes.events.domain.Topic`; the
topic channel is taken from the lower-case form of the enum's name. Multiple
event classes may share the same Topic. For example; the `USER` topic includes
the event classes `UserCreated`, `UserUpdated` and `UserDelete`.

### Consumers
To consume (listen to) events, a class implements the interface
`com.hillayes.events.consumer.EventConsumer` and specify the topic(s) they
want to consume using the annotation  `@TopicConsumer`. For example;
```
@TopicConsumer(Topic.USER)
public class UserEventConsumer implements EventConsumer {
    @Transactional
    public void consume(EventPacket eventPacket) throws Exception {
        // perform some action related to the event
        
        // use the payload class to identify the event type
        String payloadClass = eventPacket.getPayloadClass();
        if (UserCreated.class.getName().equals(payloadClass)) {
            processUserCreated(eventPacket.getPayloadContent());
        }
    }
}
```
A class may include multiple `@TopicConsumer` annotations to consume events
from multiple topics but, for the sake of coherence, it is recommended to use
a single class per topic.

### Event Delivery
Each service will maintain form own event group (as per Kafka Group). So,
if a service implements multiple `EventConsumer` classes consuming from the same
Topic, each class will only receive a portion of the events on that topic.
This is not recommended; and shouldn't be necessary anyway.

### Event Retry
If an exception is raised by the `EventConsumer` during the processing of an
event, that event will be queued for retry by the message broker and later
published to ALL consumers. Resulting in an `EventConsumer` receiving duplicate
events.
