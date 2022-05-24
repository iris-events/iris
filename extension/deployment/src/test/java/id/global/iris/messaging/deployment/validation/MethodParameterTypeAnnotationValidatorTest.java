package id.global.iris.messaging.deployment.validation;

import static id.global.common.iris.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.MessageHandler;
import id.global.iris.messaging.AbstractAnnotationInstanceValidatorTest;

class MethodParameterTypeAnnotationValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    private static final Class<MessageHandler> MESSAGE_HANDLER_CLASS = MessageHandler.class;

    private MessageAnnotationValidator messageAnnotationValidator;
    private MethodParameterTypeAnnotationValidator validator;

    @BeforeEach
    void beforeEach() {
        messageAnnotationValidator = mock(MessageAnnotationValidator.class);
        final var index = indexOf(TopicHandlerService.class, ValidTopicEvent.class);
        validator = new MethodParameterTypeAnnotationValidator(index, List.of(messageAnnotationValidator));
    }

    @Test
    void validate() {
        final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_CLASS, TopicHandlerService.class);
        assertDoesNotThrow(() -> validator.validate(annotationInstance));

        verify(messageAnnotationValidator).validate(any(AnnotationInstance.class));
    }

    private static class TopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "kebab-key" })
        public void handleTopic(ValidTopicEvent event) {
        }

    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record ValidTopicEvent() {
    }
}