package org.iris_events.deployment.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.iris_events.annotations.ExchangeType.DIRECT;
import static org.iris_events.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BindingKeyParamAnnotationValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    @ParameterizedTest
    @ValueSource(classes = { DoubleAsteriskBindingKeyTopicHandlerService.class,
            UppercaseBindingKeyTopicHandlerService.class,
            DotEndingBindingKeyTopicHandlerService.class,
            AsteriskBindingKeyDirectHandlerService.class,
            UppercaseBindingKeyDirectHandlerService.class,
            DotBindingKeyDirectHandlerService.class })
    public void validateBindingKeysAreInvalidFormat(Class<?> serviceClass) {
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
        final var validator = getValidatorService(serviceClass, ValidTopicEvent.class, ValidDirectEvent.class);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(), containsString("bindingKeys"));
        assertThat(exception.getMessage(), containsString("does not conform to the correct format"));
    }

    @ParameterizedTest
    @ValueSource(classes = { AsteriskBindingKeyTopicHandlerService.class,
            DotBindingKeyTopicHandlerService.class,
            KebabCaseBindingKeyDirectHandlerService.class
    })
    public void validateBindingKeysAreValidFormat(Class<?> serviceClass) {
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
        final var validator = getValidatorService(serviceClass, ValidTopicEvent.class, ValidDirectEvent.class);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    private BindingKeyParamAnnotationValidator getValidatorService(Class<?>... annotatedClasses) {
        final var index = indexOf(annotatedClasses);
        return new BindingKeyParamAnnotationValidator(index);
    }

    private static class DoubleAsteriskBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "wrong.**.key" })
        public void handle(ValidTopicEvent event) {
        }

    }

    private static class UppercaseBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "WRONG.upper.case" })
        public void handle(ValidTopicEvent event) {
        }

    }

    private static class DotEndingBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "wrong.end.with.dot." })
        public void handle(ValidTopicEvent event) {
        }

    }

    private static class AsteriskBindingKeyDirectHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "wrong.*.key" })
        public void handle(ValidDirectEvent event) {
        }

    }

    private static class UppercaseBindingKeyDirectHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "WRONG.upper.case" })
        public void handle(ValidDirectEvent event) {
        }

    }

    private static class DotBindingKeyDirectHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "wrong.end.with.dot" })
        public void handle(ValidDirectEvent event) {
        }

    }

    private static class AsteriskBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "valid.*.key" })
        public void handle(ValidTopicEvent event) {
        }

    }

    private static class DotBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "valid.with.dot" })
        public void handleTopic(ValidTopicEvent event) {
        }

    }

    private static class KebabCaseBindingKeyDirectHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public void handle(ValidDirectEvent event) {
        }

    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record ValidTopicEvent() {
    }

    @Message(exchangeType = DIRECT, name = "kebab-direct-exchange", routingKey = "valid.direct.event")
    public record ValidDirectEvent() {
    }
}
