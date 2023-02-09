# Collection of tools for testing IRIS event driven architecture

This repository contains some useful tools to be used for testing services using IRIS event driven architecture.

# Instructions
Include maven dependency in `test` scope as such:
```
<dependency>
    <groupId>id.global.iris</groupId>
    <artifactId>iris-test</artifactId>
    <version>${version.iris}</version>
    <scope>test</scope>
</dependency>
```

## Frontend consumer
Because IRIS does not allow sending `FRONTEND`scoped messages through its `EventProducer`bean we have provided a testing producer which allows you to do that in your test classes.

First you will need to extend the provided abstract `AbstractTestFrontendProducer` class. For example:
```java
@ApplicationScoped
public class FrontendProducer extends AbstractTestFrontendProducer {
    protected FrontendProducer() {
        super(null, null);
    }

    @Inject
    public FrontendProducer(@Named("producerChannelService") ChannelService channelService, ObjectMapper objectMapper) {
        super(channelService, objectMapper);
    }
}
```
This will make the `FrontendProducer` available to Quarkus CDI context and allow you to use this bean for testing frontend AMQP messages in `@QuarkusTest` annotated tests.
The protected no-args constructor is needed for CDI to work.

Of course you can just extend `AbstractTestFrontendProducer` without using Quarkus CDI but you will need to provide instances
of `ChannelService` and `ObjectMapper`.

Then you can inject `FrontendProducer` into your own test as follows
```java
@Inject
FrontendProducer frontendProducer;
```

And send frontend messages like this
```java
frontendProducer.send(new FrontendEvent(new AwesomePayload()));
```

## Special consumers
Another (intentional) limitation of IRIS is also the inability to consume `SESSION`, `USER` and `BROADCAST` scoped events. Because when testing your app you most likely will want to do that we have provided some special consumers for you to use.
This example will cover consuming `SESSION` scoped events.

First extend the provided abstract `AbstractTestConsumer`
```java

@ApplicationScoped
public class TestConsumer extends AbstractTestConsumer {
    protected TestConsumer() {
        super(null, null);
    }

    @Inject
    public TestConsumer(@Named("consumerChannelService") final ChannelService channelService, final ObjectMapper objectMapper) {
        super(channelService, objectMapper);
    }
}
```
This will make the `TestConsumer` bean available to Quarkus CDI and enable you to inject it into your `@QuarkusTest` annotated tests.
Again, you can do this without CDI but you will need to provide instances of `ChannelService` and `ObjectMapper`.

In your test classes you can start consumers for `SESSION` scoped events as follows:
```java
testConsumer.startSessionConsumer(YourAwesomeEvent.class, "awesome/event-name",
        (message) -> {
        final var awesomeEvent = (YourAwesomeEvent) message;
        // do assertion stuff with the event, complete a CompletableFuture or other magic
        });
```
Make sure that the event class and event-name match the event you want to test.