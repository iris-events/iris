package id.global.iris.messaging.deployment.validation;

import static id.global.iris.common.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.annotations.Scope;
import id.global.iris.messaging.AbstractAnnotationInstanceValidatorTest;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;

class MessageHandlerAnnotationInstanceValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    @Test
    void validate() {
        final var index = indexOf(EventHandlerService.class, Event.class);
        final var validator = new MessageHandlerAnnotationInstanceValidator(index, "TestService");

        final var annotationInstance = getAnnotationInstance(MessageHandler.class, EventHandlerService.class);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @ParameterizedTest
    @ValueSource(classes = { WrongBindingKeyService.class,
            WrongEventExchangeService.class,
            FrontendScopeExchangeService.class})
    void validateNotValid(Class<?> serviceClass) {
        final var index = indexOf(serviceClass, Event.class, WrongExchangeEvent.class, FrontendScopeEvent.class);
        final var validator = new MessageHandlerAnnotationInstanceValidator(index, "TestService");

        final var annotationInstance = getAnnotationInstance(MessageHandler.class, serviceClass);

        assertThrows(MessageHandlerValidationException.class, () -> validator.validate(annotationInstance));
    }

    private static class EventHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = "kebab-key")
        public void handle(Event event) {
        }

    }

    private static class WrongBindingKeyService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = "WRONG.kebab-key")
        public void handle(Event event) {
        }
    }

    private static class WrongEventExchangeService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = "kebab-key")
        public void handle(WrongExchangeEvent event) {
        }
    }

    private static class FrontendScopeExchangeService {

        @SuppressWarnings("unused")
        @MessageHandler(perInstance = true)
        public void handle(FrontendScopeEvent event) {
        }
    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record Event() {
    }

    @Message(exchangeType = TOPIC, name = "WRONG-topic-exchange", routingKey = "valid.topic.event")
    public record WrongExchangeEvent() {
    }

    @Message(name = "topic-exchange", scope = Scope.FRONTEND)
    public record FrontendScopeEvent() {
    }
}
