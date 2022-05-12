package id.global.iris.messaging.deployment.validation;

import static id.global.common.iris.annotations.ExchangeType.DIRECT;
import static id.global.common.iris.annotations.ExchangeType.TOPIC;
import static id.global.iris.messaging.deployment.constants.AnnotationInstanceParams.DEAD_LETTER_PARAM;
import static id.global.iris.messaging.deployment.constants.AnnotationInstanceParams.NAME_PARAM;
import static id.global.iris.messaging.deployment.constants.AnnotationInstanceParams.ROUTING_KEY_PARAM;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import id.global.common.auth.jwt.Role;
import id.global.common.iris.annotations.ExchangeType;
import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.MessageHandler;
import id.global.common.iris.constants.Queues;
import id.global.iris.messaging.BaseIndexingTest;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;

class AnnotationInstanceValidatorTest extends BaseIndexingTest {

    @Nested
    @DisplayName("Message handler annotation validator tests")
    class MessageHandlerTests {

        private static final Class<MessageHandler> MESSAGE_HANDLER_ANNOTATION_CLASS = MessageHandler.class;

        @Test
        public void validateParamsExist() {
            final var serviceClass = DirectEventHandlerService.class;
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validator = getValidatorService(serviceClass, ValidDirectEvent.class);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @ParameterizedTest
        @ValueSource(classes = { DoubleAsteriskBindingKeyTopicHandlerService.class,
                UppercaseBindingKeyTopicHandlerService.class,
                DotEndingBindingKeyTopicHandlerService.class })
        public void validateBindingKeysAreInValidFormat(Class<?> serviceClass) {
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validator = getValidatorService(serviceClass, ValidTopicEvent.class);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            assertThat(exception.getMessage(), containsString("bindingKeys"));
            assertThat(exception.getMessage(), containsString("does not conform to the correct format"));
        }

        @Test
        public void validateForwardedEvent() {
            final var serviceClass = ForwardedEventHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validator = getValidatorService(serviceClass, ValidDirectEvent.class, ForwardedEvent.class);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @Test
        public void validateForwardedEventWithoutAnnotation() {
            final var serviceClass = ForwardedEventWithoutAnnotationHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validator = getValidatorService(serviceClass, ValidDirectEvent.class,
                    ForwardedEventWithoutAnnotation.class);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));
            assertThat(exception.getMessage(), endsWith(
                    "must either have a return object class annotated with @Message annotation or have a void return type."));
        }

        @Test
        public void validateForwardedEventIncorrectReturnType() {
            final var serviceClass = ForwardedEventWithIncorrectReturnTypeHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validator = getValidatorService(serviceClass, ValidDirectEvent.class);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));
            assertThat(exception.getMessage(), endsWith(
                    "must either have a class or void return type."));
        }
    }

    @Nested
    @DisplayName("Consumed event annotation validator tests")
    class MessageTests {

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
    }

    private AnnotationInstanceValidator getValidatorService(Class<?>... annotatedClasses) {
        final var index = indexOf(annotatedClasses);
        return new AnnotationInstanceValidator(index, "TestService");
    }

    private AnnotationInstance getAnnotationInstance(Class<?> annotationClass, Class<?>... annotatedClasses) {
        return indexOf(annotatedClasses)
                .getAnnotations(DotName.createSimple(annotationClass.getCanonicalName()))
                .get(0);
    }

    private static class DirectEventHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(rolesAllowed = { Role.AUTHENTICATED })
        public void handle(ValidDirectEvent event) {
        }

    }

    private static class DoubleAsteriskBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "wrong.**.key" })
        public void handleTopic(ValidTopicEvent event) {
        }

    }

    private static class UppercaseBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "WRONG.upper.case" })
        public void handleTopic(ValidTopicEvent event) {
        }

    }

    private static class DotEndingBindingKeyTopicHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = { "wrong.end.with.dot." })
        public void handleTopic(ValidTopicEvent event) {
        }

    }

    @Message(name = "kebab-case-queue", routingKey = "kebab-case-queue", exchangeType = DIRECT)
    private static class ForwardedEventHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler
        public ForwardedEvent handleTopic(ValidDirectEvent event) {
            return new ForwardedEvent();
        }
    }

    private static class ForwardedEventWithoutAnnotationHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler
        public ForwardedEventWithoutAnnotation handleTopic(ValidDirectEvent event) {
            return new ForwardedEventWithoutAnnotation();
        }

    }

    private static class ForwardedEventWithIncorrectReturnTypeHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler
        public int handleTopic(ValidDirectEvent event) {
            return -1;
        }

    }

    @Message(name = "kebab-case-queue", routingKey = "kebab-case-queue", exchangeType = DIRECT, deadLetter = "dead.valid-dead-letter")
    public record ValidDirectEvent() {
    }

    @Message(exchangeType = TOPIC, name = "kebab-topic-exchange", routingKey = "valid.topic.event")
    public record ValidTopicEvent() {
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

    @Message(name = "kebab-case-queue", deadLetter = "without-prefix-dead-letter-queue")
    public record WithoutPrefixDeadLetterEvent() {
    }

    @Message(exchangeType = ExchangeType.DIRECT, name = "direct-exchange", routingKey = "direct-queue-forwarded-event")
    public record ForwardedEvent() {
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

    public record ForwardedEventWithoutAnnotation() {
    }

}
