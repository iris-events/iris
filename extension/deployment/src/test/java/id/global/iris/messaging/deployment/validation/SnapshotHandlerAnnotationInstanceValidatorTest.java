package id.global.iris.messaging.deployment.validation;

import static id.global.common.iris.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.SnapshotMessageHandler;
import id.global.common.iris.message.SnapshotRequested;
import id.global.iris.messaging.AbstractAnnotationInstanceValidatorTest;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;

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