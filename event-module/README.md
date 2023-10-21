
# One-Stop

---
## Events Library
A shared library used by those services wishing to publish and/or listen for events
on a message bus.

#### Events vs Actions
Events notify listeners that some action has occurred. The action is often the change
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
past participle. For example; SendEmail or RetrieveTransactions. They are often used
when a service needs to perform:
- long-running actions that may cause the client to wait an unacceptable length of time
  for a response
- actions that have a dependency on remote third-party services, and need to be
  repeated should communication fail

A better solution for such explicit actions would be to use some form of background
task scheduler; allowing tasks to be queued for execution, and repeated on failure.
See the `executor-lib`.


### Internal Message Bus
This uses Jakarta EE Events to all beans to communicate. The advantage is that no
third-party dependency is required. However, it only supports communication within
the application itself.

To use this event mechanism add a dependency on `local-events-lib`.

#### Event Observers
The JEE annotation `jakarta.enterprise.event.Observes` is used to annotate those
methods that wish to receive events, and the annotation
`com.hillayes.events.annotation.TopicObserved' is used identify the topic of events
to be received.

This is an example of a method to receive events from the `USER` topic. As the event
should only be processed if the originator of the event successfully commits its
transaction, the `@Observes` annotation states the `AFTER_SUCCESS` TransactionPhase.
The method is also marked as `@Transactional` as it may wish to perform database
updates of its own. Note the use of `REQUIRES_NEW`. This is because the transaction
that created the event has been closed by the time the observer method is called.
```java
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.USER) EventPacket eventPacket){
       ...
    }
```

In general, the bean class declaring this observer method will be annotated with
`@ApplicationScoped`.

#### Event Senders
The class `com.hillayes.events.sender.EventSender` from `local-events-lib` provides
methods to send events to observers on a named topic.

See `com.hillayes.user.event.UserEventSender` for an example of how the `EventSender`
is used.


### Kafka / Redpanda
The Kafka (or Redpanda) message broker can be used to communicate both internally
and externally of the application.
Those services that are only interested in listening for events, and not interested
in publishing them, only need include library `events-lib` in their dependencies.

Those services wishing to publish events to external listeners must use the "kafka"
broker, by include a dependency on the library `outbox-lib`.

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
events.

If, after the event has been retried a number of times, the event fails again,
it will be placed on the message-hospital queue (HOSPITAL_TOPIC). 
