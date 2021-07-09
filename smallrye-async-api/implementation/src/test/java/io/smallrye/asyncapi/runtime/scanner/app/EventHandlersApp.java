package io.smallrye.asyncapi.runtime.scanner.app;

import id.global.asyncapi.spec.annotations.MessageHandler;
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

    @MessageHandler
    public void handleEventV1(TestEventV1 event) {
        System.out.println("Handle event: " + event);
    }

    @MessageHandler(eventType = TestEventV2.class)
    public void handleEventV1Params(TestEventV2 event, boolean flag) {
        System.out.println("Handle event: " + event + " with flag: " + flag);
    }

    @MessageHandler()
    public SentEvent handleEventRPC(TestEventV1 event) {
        System.out.println("Handle event: " + event);
        return new SentEvent(1, "LIVE", new User("John", "Doe", 69, Status.LIVE));
    }

    @MessageHandler()
    public void handleFrontendEvent(TestFrontendEventV1 event) {
        System.out.println("Handle event: " + event);
    }
}
