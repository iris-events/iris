package io.smallrye.asyncapi.runtime.scanner.app;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;

import java.util.Map;

import id.global.common.annotations.amqp.Message;
import org.jboss.logging.Logger;

import id.global.common.annotations.amqp.MessageHandler;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersBadExampleApp.ID, info = @Info(title = EventHandlersBadExampleApp.TITLE, version = EventHandlersBadExampleApp.VERSION))
public class EventHandlersBadExampleApp {
    public static final Logger LOG = Logger.getLogger(EventHandlersBadExampleApp.class);

    public static final String TITLE = "Event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersAppTest";

    @MessageHandler(bindingKeys = "hashmap-queue")
    public void handleHashMapEvent(MapEvent event) {
        LOG.info("Handle hash map event: " + event);
    }

    @Message(exchangeType = DIRECT)
    public record MapEvent(Map<String, Object> mapProperty) {
    }
}
