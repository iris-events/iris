package org.iris_events.asyncapi.runtime.scanner.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.iris_events.asyncapi.spec.annotations.media.SchemaProperty;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;

public class ParseErrorEventHandlersApp {
    private static final Logger LOG = LoggerFactory.getLogger(ParseErrorEventHandlersApp.class);

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleEventWithDescribedEnumProperty(EventWithDescribedEnum event) {
        LOG.info("Handling event with described enum");
    }

    @Message(name = "event-with-described-enum")
    public record EventWithDescribedEnum(@SchemaProperty(description = "Requirement to verify.") TestType type) {
    }

    public enum TestType {
        FOO,
        BAR
    }
}
