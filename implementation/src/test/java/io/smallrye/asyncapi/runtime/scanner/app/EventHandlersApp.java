package io.smallrye.asyncapi.runtime.scanner.app;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.SentEvent;
import io.smallrye.asyncapi.runtime.scanner.model.Status;
import io.smallrye.asyncapi.runtime.scanner.model.TestEventV1;
import io.smallrye.asyncapi.runtime.scanner.model.TestEventV2;
import io.smallrye.asyncapi.runtime.scanner.model.TestFrontendEventV1;
import io.smallrye.asyncapi.runtime.scanner.model.User;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersApp.ID, info = @Info(title = EventHandlersApp.TITLE, version = EventHandlersApp.VERSION))
public class EventHandlersApp {
    public static final String TITLE = "Event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersAppTest";

    @MessageHandler(queue = "defaultTestEventV1")
    public void handleEventV1(TestEventV1 event) {
        System.out.println("Handle event: " + event);
    }

    @MessageHandler(queue = "testEventV2", eventType = TestEventV2.class)
    public void handleEventV1Params(TestEventV2 event, boolean flag) {
        System.out.println("Handle event: " + event + " with flag: " + flag);
    }

    @MessageHandler(queue = "rpcTestEventV1")
    public SentEvent handleEventRPC(TestEventV1 event) {
        System.out.println("Handle event: " + event);
        return new SentEvent(1, "LIVE", new User("John", "Doe", 69, Status.LIVE));
    }

    @MessageHandler(queue = "feTestEventV1")
    public void handleFrontendEvent(TestFrontendEventV1 event) {
        System.out.println("Handle event: " + event);
    }

    @TopicMessageHandler(exchange = "test_topic_exchange", bindingKeys = { "*.*.rabbit", "fast.orange.*" })
    public void handleTopicEvent(TestEventV1 event) {
        System.out.println("Handling topic event");
    }

    @FanoutMessageHandler(exchange = "test_fanout_exchange")
    public void handleFanoutEvent(TestEventV1 event) {
        System.out.println("Handling fanout event");
    }
}
