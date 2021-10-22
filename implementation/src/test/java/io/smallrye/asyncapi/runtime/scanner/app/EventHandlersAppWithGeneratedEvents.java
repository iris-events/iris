package io.smallrye.asyncapi.runtime.scanner.app;

import org.jboss.logging.Logger;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.User;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersAppWithGeneratedEvents.ID, info = @Info(title = EventHandlersAppWithGeneratedEvents.TITLE, version = EventHandlersAppWithGeneratedEvents.VERSION))
public class EventHandlersAppWithGeneratedEvents {
    private static final Logger LOG = Logger.getLogger(EventHandlersAppWithGeneratedEvents.class);

    public static final String TITLE = "Event handlers with generated events";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersGeneratedEventAppTest";

    @MessageHandler
    public void handleEventV1(SimulatedGeneratedEvent event) {
        LOG.info("Handle event: " + event);
    }

    @ConsumedEvent(queue = "generated-event")
    public record SimulatedGeneratedEvent(int id, String status, User user) {
    }
}
