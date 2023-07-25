package org.iris_events.asyncapi.runtime.scanner.app;

import java.util.Map;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandlersAppWithMapProperty {
    private static final Logger LOG = LoggerFactory.getLogger(EventHandlersAppWithMapProperty.class);

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleEventWithDescribedEnumProperty(EventWithMapValue event) {
        LOG.info("Handling event with map value");
    }

    @Message(name = "event-with-map-value")
    public record EventWithMapValue(Map<Integer, Foo> fooMap) {
    }

    public record Foo(int id, String bar) {
    }
}
