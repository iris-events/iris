package id.global.iris.messaging.deployment.validation;

import static id.global.iris.common.annotations.ExchangeType.TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.messaging.AbstractAnnotationInstanceValidatorTest;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;

class MethodReturnTypeAnnotationValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    private static final Class<MessageHandler> MESSAGE_HANDLER_CLASS = MessageHandler.class;

    @Test
    void validate() {
        final var messageAnnotationValidator = mock(MessageAnnotationValidator.class);
        final var index = indexOf(ReturnTypeHandlerService.class, ValidTopicEvent.class, ValidReturnTopicEvent.class);
        final var validator = new MethodReturnTypeAnnotationValidator(index, messageAnnotationValidator);

        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS, ReturnTypeHandlerService.class);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));

        verify(messageAnnotationValidator).validate(any(AnnotationInstance.class));
    }

    @Test
    void validateReturnTypeVoid() {
        final var messageAnnotationValidator = mock(MessageAnnotationValidator.class);
        final var index = indexOf(ReturnTypeVoidTopicHandlerService.class, ValidTopicEvent.class);
        final var validator = new MethodReturnTypeAnnotationValidator(index, messageAnnotationValidator);

        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS, ReturnTypeVoidTopicHandlerService.class);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));

        verifyNoInteractions(messageAnnotationValidator);
    }

    @ParameterizedTest
    @ValueSource(classes = { ReturnTypePrimitiveTopicHandlerService.class,
            ReturnTypeArrayTopicHandlerService.class,
            ReturnTypeWildcardTopicHandlerService.class,
            ReturnTypeParameterizedTopicHandlerService.class
    })
    void validateReturnTypeNotClass(Class<?> serviceClass) {
        final var messageAnnotationValidator = mock(MessageAnnotationValidator.class);
        final var index = indexOf(serviceClass, ValidTopicEvent.class);
        final var validator = new MethodReturnTypeAnnotationValidator(index, messageAnnotationValidator);

        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS, serviceClass);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(), containsString("must either have a class or void return type."));
        verifyNoInteractions(messageAnnotationValidator);
    }

    @Test
    void validateReturnTypeWithoutAnnotation() {
        final var messageAnnotationValidator = mock(MessageAnnotationValidator.class);
        final var index = indexOf(ReturnParamWithoutAnnotationTopicHandlerService.class, NotValidEvent.class);
        final var validator = new MethodReturnTypeAnnotationValidator(index, messageAnnotationValidator);

        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS,
                ReturnParamWithoutAnnotationTopicHandlerService.class);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(), containsString(
                "must either have a return object class annotated with @Message annotation or have a void return type."));
        verifyNoInteractions(messageAnnotationValidator);
    }

    private static class ReturnTypeVoidTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public void handle(ValidTopicEvent event) {
        }

    }

    private static class ReturnTypePrimitiveTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public int handleTopic(ValidTopicEvent event) {
            return 1;
        }

    }

    private static class ReturnTypeArrayTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public int[] handle(ValidTopicEvent event) {
            return new int[] {};
        }

    }

    private static class ReturnTypeWildcardTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public List<?> handle(ValidTopicEvent event) {
            return List.of();
        }

    }

    private static class ReturnTypeParameterizedTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public List<String> handle(ValidTopicEvent event) {
            return List.of();
        }

    }

    private static class ReturnParamWithoutAnnotationTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public NotValidEvent handle(ValidTopicEvent event) {
            return new NotValidEvent();
        }

    }

    private static class ReturnTypeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public ValidReturnTopicEvent handle(ValidTopicEvent event) {
            return new ValidReturnTopicEvent();
        }

    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record ValidTopicEvent() {
    }

    @Message(exchangeType = TOPIC, name = "return-topic-exchange", routingKey = "valid.topic.event")
    public record ValidReturnTopicEvent() {
    }

    public record NotValidEvent() {
    }
}
