package org.iris_events.deployment.validation;

import static org.iris_events.annotations.ExchangeType.DIRECT;
import static org.iris_events.annotations.ExchangeType.TOPIC;
import static org.iris_events.deployment.constants.AnnotationInstanceParams.DEAD_LETTER_PARAM;
import static org.iris_events.deployment.constants.AnnotationInstanceParams.NAME_PARAM;
import static org.iris_events.deployment.constants.AnnotationInstanceParams.ROUTING_KEY_PARAM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.iris_events.annotations.Message;
import org.iris_events.common.Queues;
import org.iris_events.AbstractAnnotationInstanceValidatorTest;
import org.iris_events.deployment.MessageHandlerValidationException;

class MessageAnnotationValidatorTest extends AbstractAnnotationInstanceValidatorTest {

    private static final Class<Message> CONSUMED_EVENT_ANNOTATION_CLASS = Message.class;

    @Test
    void validate() {
        final var eventClass = ValidDirectEvent.class;
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = getValidatorService(eventClass);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @ParameterizedTest
    @MethodSource
    public void validateMessageParamsExist(Class<?> eventClass) {
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = getValidatorService(eventClass);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> validateMessageParamsExist() {
        return Stream.of(
                Arguments.of(ValidDirectEvent.class),
                Arguments.of(ValidTopicEvent.class));
    }

    @ParameterizedTest
    @MethodSource
    public void validateMessageParamsAreKebabCase(Class<?> eventClass) {
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = getValidatorService(eventClass);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> validateMessageParamsAreKebabCase() {
        return Stream.of(Arguments.of(ValidDirectEvent.class), Arguments.of(ValidTopicEvent.class));
    }

    @ParameterizedTest
    @MethodSource
    public void validateMessageParamsAreNotKebabCase(Class<?> eventClass, Set<String> requiredKebabCaseParams) {
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = getValidatorService(eventClass);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        requiredKebabCaseParams.forEach(parameter -> assertThat(exception.getMessage(), containsString(parameter)));
        assertThat(exception.getMessage(), containsString("formatted in kebab case."));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> validateMessageParamsAreNotKebabCase() {
        return Stream.of(
                Arguments.of(CamelCaseDirectEvent.class, Set.of(ROUTING_KEY_PARAM)),
                Arguments.of(NonKebabExchangeTopicEvent.class, Set.of(NAME_PARAM)),
                Arguments.of(NonKebabDeadLetterEvent.class, Set.of(DEAD_LETTER_PARAM)));
    }

    @ParameterizedTest
    @MethodSource
    public void validateReservedNames(Class<?> eventClass) {
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = getValidatorService(eventClass);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(), containsString("is using reserved names"));
        assertThat(exception.getMessage(), containsString(annotationInstance.name().toString()));
        assertThat(exception.getMessage(), containsString(eventClass.getName()));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> validateReservedNames() {
        return Stream.of(
                Arguments.of(RetryNameEvent.class),
                Arguments.of(RetryRoutingKeyEvent.class),
                Arguments.of(ErrorNameEvent.class),
                Arguments.of(ErrorRoutingKeyEvent.class));
    }

    @Test
    void validateDeadLetterWithoutPrefix() {
        final var eventClass = WithoutPrefixDeadLetterEvent.class;
        final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
        final var validator = getValidatorService(eventClass);

        final var exception = assertThrows(MessageHandlerValidationException.class,
                () -> validator.validate(annotationInstance));

        assertThat(exception.getMessage(),
                containsString("must start with the prefix \"" + Queues.Constants.DEAD_LETTER_PREFIX + "\"."));
        assertThat(exception.getMessage(), containsString(annotationInstance.name().toString()));
        assertThat(exception.getMessage(), containsString(eventClass.getName()));
    }

    private MessageAnnotationValidator getValidatorService(Class<?>... annotatedClasses) {
        final var index = indexOf(annotatedClasses);
        return new MessageAnnotationValidator("TestService", index);
    }

    @Message(name = "kebab-case-queue", routingKey = "kebab-case-queue", exchangeType = DIRECT, deadLetter = "dead.valid-dead-letter")
    public record ValidDirectEvent() {
    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record ValidTopicEvent() {
    }

    @Message(name = "kebab-case-queue", deadLetter = "without-prefix-dead-letter-queue")
    public record WithoutPrefixDeadLetterEvent() {
    }

    @Message(name = "camel-case-queue", routingKey = "CamelCaseQueue", exchangeType = DIRECT)
    public record CamelCaseDirectEvent() {
    }

    @Message(exchangeType = TOPIC, name = "NonKebabExchange", routingKey = "non.kebab.exchange.topic")
    public record NonKebabExchangeTopicEvent() {
    }

    @Message(name = "kebab-case-queue", deadLetter = "nonKebabCaseDeadLetter")
    public record NonKebabDeadLetterEvent() {
    }

    @Message(name = "retry")
    public record RetryNameEvent() {
    }

    @Message(name = "valid", routingKey = "retry")
    public record RetryRoutingKeyEvent() {
    }

    @Message(name = "error")
    public record ErrorNameEvent() {
    }

    @Message(name = "alsoValid", routingKey = "error")
    public record ErrorRoutingKeyEvent() {
    }
}
