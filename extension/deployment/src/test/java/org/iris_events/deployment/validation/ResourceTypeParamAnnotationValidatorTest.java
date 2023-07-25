package org.iris_events.deployment.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.common.message.SnapshotRequested;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceTypeParamAnnotationValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    @ParameterizedTest
    @ValueSource(classes = { AsteriskResourceTypeHandlerService.class,
            UppercaseResourceTypeHandlerService.class,
            DotResourceTypeHandlerService.class })
    public void validateBindingKeysAreInvalidFormat(Class<?> serviceClass) {
        final var annotationClass = SnapshotMessageHandler.class;

        final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
        final var validator = new ResourceTypeParamAnnotationValidator();

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(),
                containsString("class requires parameter \"resourceType\" to be formatted in kebab case."));
    }

    @ParameterizedTest
    @ValueSource(classes = { KebabCaseResourceTypeHandlerService.class })
    public void validateBindingKeysAreValidFormat(Class<?> serviceClass) {
        final var annotationClass = SnapshotMessageHandler.class;

        final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
        final var validator = new ResourceTypeParamAnnotationValidator();

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    private static class AsteriskResourceTypeHandlerService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "wrong.*.key")
        public void handle(SnapshotRequested event) {
        }

    }

    private static class UppercaseResourceTypeHandlerService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "WRONG-upper-case")
        public void handle(SnapshotRequested event) {
        }

    }

    private static class DotResourceTypeHandlerService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "wrong.with.dot")
        public void handle(SnapshotRequested event) {
        }

    }

    private static class KebabCaseResourceTypeHandlerService {

        @SuppressWarnings("unused")
        @SnapshotMessageHandler(resourceType = "kebab-key")
        public void handle(SnapshotRequested event) {
        }

    }

}
