package org.iris_events.deployment.validation;

import static org.iris_events.annotations.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.jboss.jandex.AnnotationInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
