package id.global.event.messaging.deployment.validation;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import id.global.common.annotations.amqp.ConsumedEvent;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.annotations.amqp.ProducedEvent;
import id.global.event.messaging.BaseIndexingTest;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

class AnnotationInstanceValidatorTest extends BaseIndexingTest {

    @Nested
    @DisplayName("Message handler annotation validator tests")
    class MessageHandlerTests {

        private static final Class<MessageHandler> MESSAGE_HANDLER_ANNOTATION_CLASS = MessageHandler.class;
        private static final Class<ConsumedEvent> CONSUMED_EVENT_ANNOTATION_CLASS = ConsumedEvent.class;

        @ParameterizedTest
        @MethodSource
        public void validateParamCountIsCorrect(Class<?> serviceClass, int paramCount) {
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validationRules = new ValidationRules(paramCount, true);
            final var validator = getValidatorService(validationRules, serviceClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateParamCountIsCorrect() {
            return Stream.of(
                    Arguments.of(DirectEventHandlerService.class, 1),
                    Arguments.of(TopicEventHandlerService.class, 1),
                    Arguments.of(DirectEventHandlerServiceWithMultipleArguments.class, 2));
        }

        @ParameterizedTest
        @MethodSource
        public void validateParamsCountIsNotCorrect(Class<?> serviceClass, int paramCount) {
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validationRules = new ValidationRules(paramCount, true);
            final var validator = getValidatorService(validationRules, serviceClass);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            assertThat(exception.getMessage(),
                    endsWith(" must declare exactly " + paramCount + " parameters that represents the event."));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateParamsCountIsNotCorrect() {
            return Stream.of(
                    Arguments.of(DirectEventHandlerService.class, 2),
                    Arguments.of(TopicEventHandlerService.class, 3),
                    Arguments.of(DirectEventHandlerServiceWithMultipleArguments.class, 1));
        }

        @Test
        public void validateParamsExist() {
            final var serviceClass = DirectEventHandlerService.class;
            final var validationRules = new ValidationRules(null, true);
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validator = getValidatorService(validationRules, serviceClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @Test
        public void validateParamsAreMissing() {
            final var topicEventClass = TopicEventMissingParams.class;
            final var validationRules = new ValidationRules(null, true);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, topicEventClass);
            final var validator = getValidatorService(validationRules, topicEventClass);

            final var missingParamException = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            assertThat(missingParamException.getMessage(),
                    containsString(String.format("Parameter(s) \"%s\" missing in annotation", BINDING_KEYS_PARAM)));
        }

        @Test
        public void validateParamExternalDependency() {
            final var serviceClass = DirectEventHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validationRules = new ValidationRules(null, false);
            final var indexWithEventAsExternalDependency = indexOf(serviceClass);
            final var validator = getValidatorService(indexWithEventAsExternalDependency, validationRules);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));
            assertThat(exception.getMessage(), endsWith("can not have external dependency classes as parameters."));
        }

        @Test
        public void validateForwardedEvent() {
            final var serviceClass = ForwardedEventHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validationRules = new ValidationRules(null, true);
            final var indexWithEventAsExternalDependency = indexOf(serviceClass, ForwardedEvent.class);
            final var validator = getValidatorService(indexWithEventAsExternalDependency, validationRules);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @Test
        public void validateForwardedEventWithoutAnnotation() {
            final var serviceClass = ForwardedEventWithoutAnnotationHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validationRules = new ValidationRules(null, true);
            final var indexWithEventAsExternalDependency = indexOf(serviceClass, ForwardedEventWithoutAnnotation.class);
            final var validator = getValidatorService(indexWithEventAsExternalDependency, validationRules);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));
            assertThat(exception.getMessage(), endsWith(
                    "must either have a return object class annotated with @ProducedEvent annotation or have a void return type."));
        }

        @Test
        public void validateForwardedEventIncorrectReturnType() {
            final var serviceClass = ForwardedEventWithIncorrectReturnTypeHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validationRules = new ValidationRules(null, true);
            final var indexWithEventAsExternalDependency = indexOf(serviceClass, ForwardedEventWithoutAnnotation.class);
            final var validator = getValidatorService(indexWithEventAsExternalDependency, validationRules);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));
            assertThat(exception.getMessage(), endsWith(
                    "must either have a class or void return type."));
        }
    }

    @Nested
    @DisplayName("Consumed event annotation validator tests")
    class ConsumedEventTests {

        private static final Class<ConsumedEvent> CONSUMED_EVENT_ANNOTATION_CLASS = ConsumedEvent.class;

        @Test
        void validateWithRules() {
            final var eventClass = ValidDirectEvent.class;
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validationRules = new ValidationRules(
                    1,
                    false);

            final var validator = getValidatorService(validationRules, eventClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsExist(Class<?> eventClass) {
            final var validationRules = new ValidationRules(null, false);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsExist() {
            return Stream.of(
                    Arguments.of(ValidDirectEvent.class),
                    Arguments.of(ValidTopicEvent.class));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsAreKebabCase(Class<?> eventClass) {
            final var validationRules = new ValidationRules(null, false);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsAreKebabCase() {
            return Stream.of(Arguments.of(ValidDirectEvent.class), Arguments.of(ValidTopicEvent.class));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsAreNotKebabCase(Class<?> eventClass, Set<String> requiredKebabCaseParams) {
            final var validationRules = new ValidationRules(null, false);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            requiredKebabCaseParams.forEach(parameter -> assertThat(exception.getMessage(), containsString(parameter)));
            assertThat(exception.getMessage(), containsString("formatted in kebab case."));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsAreNotKebabCase() {
            return Stream.of(
                    Arguments.of(CamelCaseDirectEvent.class, Set.of(BINDING_KEYS_PARAM)),
                    Arguments.of(NonKebabExchangeTopicEvent.class, Set.of(EXCHANGE_PARAM)));
        }

        @ParameterizedTest
        @ValueSource(classes = { DoubleAsteriskBindingKey.class,
                UppercaseBindingKeyTopicEvent.class,
                DotEndingBindingKeyTopicEvent.class })
        public void validateBindingKeysAreInValidFormat(Class<?> eventClass) {
            final var validationRules = new ValidationRules(null, false);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            assertThat(exception.getMessage(), containsString("bindingKeys"));
            assertThat(exception.getMessage(), containsString("does not conform to the correct format"));
        }
    }

    private AnnotationInstanceValidator getValidatorService(ValidationRules validationRules, Class<?>... annotatedClasses) {
        return getValidatorService(indexOf(annotatedClasses), validationRules);
    }

    private AnnotationInstanceValidator getValidatorService(Index index, ValidationRules validationRules) {
        return new AnnotationInstanceValidator(index, validationRules);
    }

    private AnnotationInstance getAnnotationInstance(Class<?> annotationClass, Class<?>... annotatedClasses) {
        return indexOf(annotatedClasses)
                .getAnnotations(DotName.createSimple(annotationClass.getCanonicalName()))
                .get(0);
    }

    private static class DirectEventHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler(rolesAllowed = { "USER" })
        public void handle(ValidDirectEvent event) {
        }

    }

    private static class DirectEventHandlerServiceWithMultipleArguments {

        @SuppressWarnings("unused")
        @MessageHandler
        public void handle(ValidDirectEvent event, String string) {
        }

    }

    private static class TopicEventHandlerService {

        @SuppressWarnings("unused")
        @MessageHandler
        public void handleTopic(ValidTopicEvent event) {
        }

    }

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

    @ConsumedEvent(bindingKeys = "kebab-case-queue", exchangeType = DIRECT)
    public record ValidDirectEvent() {
    }

    @ConsumedEvent(exchangeType = TOPIC)
    public record TopicEventMissingParams() {
    }

    @ConsumedEvent(exchangeType = TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "test.*.key",
            "test.no.wildcard",
            "testsimple", "test.end.with.wildcard.*" })
    public record ValidTopicEvent() {
    }

    @ConsumedEvent(bindingKeys = "CamelCaseQueue", exchangeType = DIRECT)
    public record CamelCaseDirectEvent() {
    }

    @ConsumedEvent(exchangeType = TOPIC, exchange = "NonKebabExchange", bindingKeys = { "wrong.**.key" })
    public record NonKebabExchangeTopicEvent() {
    }

    @ConsumedEvent(exchangeType = TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "wrong.**.key" })
    public record DoubleAsteriskBindingKey() {
    }

    @ConsumedEvent(exchangeType = TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "WRONG.upper.case" })
    public record UppercaseBindingKeyTopicEvent() {
    }

    @ConsumedEvent(exchangeType = TOPIC, exchange = "kebab-topic-exchange", bindingKeys = {
            "wrong.end.with.dot." })
    public record DotEndingBindingKeyTopicEvent() {
    }

    @ProducedEvent(exchangeType = ExchangeType.DIRECT, exchange = "direct-exchange", routingKey = "direct-queue-forwarded-event")
    public record ForwardedEvent() {

    }

    public record ForwardedEventWithoutAnnotation() {
    }
}
