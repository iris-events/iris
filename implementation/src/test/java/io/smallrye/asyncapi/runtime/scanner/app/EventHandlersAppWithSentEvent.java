package io.smallrye.asyncapi.runtime.scanner.app;

import org.jboss.logging.Logger;

import id.global.asyncapi.spec.annotations.MessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.SentEvent;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersAppWithSentEvent.ID, info = @Info(title = EventHandlersAppWithSentEvent.TITLE, version = EventHandlersAppWithSentEvent.VERSION))
public class EventHandlersAppWithSentEvent {
    private static final Logger LOG = Logger.getLogger(EventHandlersAppWithSentEvent.class);

    public static final String TITLE = "Event handlers with sent events";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersSentEventAppTest";

    @MessageHandler(queue = "sentEventV1")
    public void handleEventV1(SentEvent event) {
        LOG.info("Handle event: " + event);
    }
}
