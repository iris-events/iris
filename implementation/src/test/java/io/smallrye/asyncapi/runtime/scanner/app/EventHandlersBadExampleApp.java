package io.smallrye.asyncapi.runtime.scanner.app;

import id.global.asyncapi.spec.annotations.MessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.MapEvent;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersBadExampleApp.ID, info = @Info(title = EventHandlersBadExampleApp.TITLE, version = EventHandlersBadExampleApp.VERSION))
public class EventHandlersBadExampleApp {
    public static final String TITLE = "Event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersAppTest";

    @MessageHandler(queue = "hashmapQueue")
    public void handleHashMapEvent(MapEvent event) {
        System.out.println("Handle hash map event: " + event);
    }
}
