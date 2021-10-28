package io.smallrye.asyncapi.runtime.scanner.app;

import org.jboss.logging.Logger;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.asyncapi.spec.enums.Scope;
import io.smallrye.asyncapi.runtime.scanner.model.User;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersAppWithSentEvent.ID, info = @Info(title = EventHandlersAppWithSentEvent.TITLE, version = EventHandlersAppWithSentEvent.VERSION))
public class EventHandlersAppWithSentEvent {
    private static final Logger LOG = Logger.getLogger(EventHandlersAppWithSentEvent.class);

    public static final String TITLE = "Event handlers with sent events";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersSentEventAppTest";

    @MessageHandler
    public void handleEventV1(SentEvent event) {
        LOG.info("Handle event: " + event);
    }

    @ProducedEvent(
            exchange = "sent-event-exchange",
            queue = "sent-event-queue",
            scope = Scope.EXTERNAL,
            exchangeType = ExchangeType.DIRECT,
            rolesAllowed = { "ADMIN", "USER", "DUMMY" })
    @ConsumedEvent(queue = "sent-event-v1")
    public record SentEvent(int id, String status, User user) {
    }

}
