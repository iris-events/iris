package org.iris_events.deployment.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;

import org.iris_events.annotations.Message;
import org.iris_events.common.message.SnapshotRequested;
import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.deployment.MessageHandlerValidationException;

class AllowedMessageValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    private static final Class<Message> CONSUMED_EVENT_ANNOTATION_CLASS = Message.class;
    private static final DotName SNAPSHOT_REQUESTED_DOT_NAME = DotName.createSimple(SnapshotRequested.class.getCanonicalName());

    @Test
    void validate() {
        final var eventClass = SnapshotRequested.class;
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = new AllowedMessageValidator(List.of(SNAPSHOT_REQUESTED_DOT_NAME));

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @Test
    void validateNotValidEvent() {
        final var eventClass = NotValidEvent.class;
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = new AllowedMessageValidator(List.of(SNAPSHOT_REQUESTED_DOT_NAME));

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));
        assertThat(exception.getMessage(), containsString("classes should be used as a parameter for the message handler."));
    }

    @Message(name = "kebab-case-queue")
    public record NotValidEvent() {
    }
}
