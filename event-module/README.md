
# One-Stop

---
## Events Library
A shared library used by those services wishing to listen for events published
by the message broker (i.e. `outbox-lib`). Those services that are only interested
in listening for events, and not interested in publishing them, only need
include the `events-lib` in their dependencies.

Those services wishing to publish events must also include a dependency on the
library `outbox-lib`.

#### Events vs Actions
Events notify listeners that something has occurred. The event is often the change
in an entity's state. For example; a new user may have been created, or a user may
have given consent to access their bank account.

By that definition, events are normally named as past participles of the actions or
state change that caused them. To follow the previous examples, the event classes
might be UserCreated and ConsentGiven.

Actions are what cause events to occur. For example; "create new user", "get user's
consent". Actions *can* be triggered as a result of an event, but the sender of the
event need not (and, ideally, should not) be aware of those actions. These actions
should not be an explicit part of the use-case that changes an entity's state. Instead,
they should be performed as implicit use-cases that are triggered whenever a particular
change to the entity's state occurs.

For example; the use-case for "get user's consent" may be:
- Given: a user provides consent to access their bank account
- When: the consent is posted to the consent service
- Then: record the user's consent in the database
- And: raise a ConsentGiven event

The use-cases for the ConsentGiven event may be:
- Given: a ConsentGiven event has been raised
- When: that event is received by the email service
- Then: the user is sent a confirmation email

and:
- Given: a ConsentGiven event has been raised
- When: that event is received by the account service
- Then: the transactions are retrieved from the user's bank account

The first use-case need have no mention of the others and the other use-case need have
no knowledge of what action raised the event. This allows services to communicate
state changes across boundaries without tightly coupling those services. It also allows
future requirements to extend what actions are to be performed on a given state change
without requiring code changes to the existing actions.

Events should not be used to trigger explicit, or imperative, actions. If, for example,
we rewrote the above use-case.

- Given: a user provides consent to access their bank account
- When: the consent is posted to the consent service
- Then: record the user's consent in the database
- And: raise an event to send a confirmation email
- And: raise an event to retrieve the user's bank account transactions

We now have a coupling between the services. The consent service knows that the email
service and account service need to perform explicit actions when a user's consent is
given. Also, any new actions would require changes to the consent service.

These explicit events can often be recognised by the fact that their name is not a
past participle. For example; SendEmail or RetrieveTransactions.

Actions are often used when a service needs to perform: 
- long-running tasks that may cause the client to wait an unacceptable length of time
for a response
- a task that has a dependency on remote third-party services, and need to be repeated
should communication fail

A better solution for such explicit actions would be to use some form of background
task scheduler; allowing tasks to be queued for execution, and repeated on failure.
See the `executor-lib`.

### Topics
Event topics are defined in the enum `com.hillayes.events.domain.Topic`; the
topic channel is taken from the lower-case form of the enum's name. Multiple
event classes may share the same Topic. For example; the `USER` topic includes
the event classes `UserCreated`, `UserUpdated` and `UserDelete`.

### Topic Groups
EventConsumers can also indicate the consumer group they belong to using the
annotation `@ConsumerGroup`.

Events will be sent to all consumer groups. Within a consumer group, events of
the same topic will be distributed to those topic consumers within that group;
with only one consumer in the group receiving a given event instance. If an event
is retried (see later), it may be allocated to another consumer in the group.

By default, the consumer group is taken from the configuration property "kafka.group.id"
or, if that property is not defined, the application/service name.

By specifying an explicit consumer group (using the annotation `@ConsumerGroup`)
events from the same topic can be distributed across consumers within different
application/service boundaries.

### Modules
#### events-common
This library contains the common classes used by both event publishers and
consumers.
#### events-lib
This library contains the classes used by event consumers, and should be included
in the dependencies of any service that wishes to listen for events.
The class `ConsumerProxy` receives events from the message broker and forwards
them to the appropriate `EventConsumer` class. If an exception is raised by the
`EventConsumer`, the event is queued for retry by the message broker; or, if the
event has been retried a number of times, it is placed on the `message-hospital`
topic.
#### outbox-lib
This module contains the classes to queue and publish events, and should be
included in the dependencies of any service that wishes to publish events.
It includes the class `EventSender` used to queue events for publishing. The
class `EventDeliverer` will periodically check the event queue and publish
any queued events to the message broker.

When a service includes a dependency on this library two tables will be added
to that service's database schema; `events` and `message_hospital`.

### Consumers
To consume (listen to) events, a class implements the interface
`com.hillayes.events.consumer.EventConsumer` and specifies the topic(s) to be
consumed using the annotation  `@TopicConsumer`. For example;
```Java
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
events. Consumers must ensure that they are idempotent.

If, after the event has been retried a number of times, the event fails again,
it will be placed on the message-hospital queue (HOSPITAL_TOPIC). 
