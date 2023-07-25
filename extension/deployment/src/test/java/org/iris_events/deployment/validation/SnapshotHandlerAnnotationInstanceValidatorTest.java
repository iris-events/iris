package org.iris_events.deployment.validation;

import static org.iris_events.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.common.message.SnapshotRequested;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnapshotHandlerAnnotationInstanceValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    @Test
    void validate() {
        final var index = indexOf(EventHandlerService.class, SnapshotRequested.class);
        final var validator = new SnapshotHandlerAnnotationInstanceValidator(index, "TestService");

        final var annotationInstance = getAnnotationInstance(SnapshotMessageHandler.class, EventHandlerService.class);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @ParameterizedTest
    @ValueSource(classes = { ResourceTypeWrongFormatService.class,
            NotAllowedEventService.class })
    void validateNotValid(Class<?> serviceClass) {
        final var index = indexOf(serviceClass, SnapshotRequested.class, NotAllowedEvent.class);
        final var validator = new SnapshotHandlerAnnotationInstanceValidator(index, "TestService");

        final var annotationInstance = getAnnotationInstance(SnapshotMessageHandler.class, serviceClass);

        assertThrows(MessageHandlerValidationException.class, () -> validator.validate(annotationInstance));
    }

    private static class EventHandlerService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "kebab-key")
        public void handle(SnapshotRequested snapshotRequested) {
        }

    }

    private static class ResourceTypeWrongFormatService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "WRONG.type")
        public void handle(SnapshotRequested snapshotRequested) {
        }
    }

    private static class NotAllowedEventService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "WRONG.type")
        public void handle(NotAllowedEvent event) {
        }
    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record NotAllowedEvent() {
    }
}
