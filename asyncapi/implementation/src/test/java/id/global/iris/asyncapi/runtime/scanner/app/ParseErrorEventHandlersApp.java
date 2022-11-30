package id.global.iris.asyncapi.runtime.scanner.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.iris.asyncapi.spec.annotations.media.SchemaProperty;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;

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
