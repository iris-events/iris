package io.smallrye.asyncapi.runtime.scanner.app;

import java.util.Map;

import org.jboss.logging.Logger;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersBadExampleApp.ID, info = @Info(title = EventHandlersBadExampleApp.TITLE, version = EventHandlersBadExampleApp.VERSION))
public class EventHandlersBadExampleApp {
    public static final Logger LOG = Logger.getLogger(EventHandlersBadExampleApp.class);

    public static final String TITLE = "Event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersAppTest";

    @MessageHandler
    public void handleHashMapEvent(MapEvent event) {
        LOG.info("Handle hash map event: " + event);
    }

    @ConsumedEvent(queue = "hashmapQueue")
    public record MapEvent(Map<String, Object> mapProperty) {
    }
}
