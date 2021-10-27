package id.global.event.messaging.deployment.validation;

import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_TYPE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.QUEUE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.ROLES_ALLOWED_PARAM;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
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

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.event.messaging.BaseIndexingTest;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

class AnnotationInstanceValidatorTest extends BaseIndexingTest {

    @Nested
    @DisplayName("Message handler annotation validator tests")
    class MessageHandlerTests {

        private static final Class<MessageHandler> MESSAGE_HANDLER_ANNOTATION_CLASS = MessageHandler.class;

        @ParameterizedTest
        @MethodSource
        public void validateParamCountIsCorrect(Class<?> serviceClass, int paramCount) {
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validationRules = new ValidationRules(paramCount, true, false, null, null);
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
            final var validationRules = new ValidationRules(paramCount, true, false, null, null);
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
            final var requiredParams = Set.of(ROLES_ALLOWED_PARAM);
            final var validationRules = new ValidationRules(null, true, false, requiredParams, null);
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validator = getValidatorService(validationRules, serviceClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @Test
        public void validateParamsAreMissing() {
            final var serviceClass = TopicEventHandlerService.class;
            final var requiredParam = ROLES_ALLOWED_PARAM;
            final var validationRules = new ValidationRules(null, true, false, Set.of(requiredParam), null);
            final var annotationInstance = getAnnotationInstance(MESSAGE_HANDLER_ANNOTATION_CLASS, serviceClass);
            final var validator = getValidatorService(validationRules, serviceClass);

            final var missingParamException = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            assertThat(missingParamException.getMessage(), containsString(requiredParam));
            assertThat(missingParamException.getMessage(), containsString("missing on MessageHandler annotation"));
        }

        @Test
        public void validateParamExternalDependency() {
            final var serviceClass = DirectEventHandlerService.class;
            final var annotationClass = MessageHandler.class;

            final var annotationInstance = getAnnotationInstance(annotationClass, serviceClass);
            final var validationRules = new ValidationRules(null, false, false, null, null);
            final var indexWithEventAsExternalDependency = indexOf(serviceClass);
            final var validator = getValidatorService(indexWithEventAsExternalDependency, validationRules);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));
            assertThat(exception.getMessage(), endsWith("can not have external dependency classes as parameters."));
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
                    false,
                    false,
                    Set.of(QUEUE_PARAM),
                    Set.of(QUEUE_PARAM));

            final var validator = getValidatorService(validationRules, eventClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsExist(Class<?> eventClass, Set<String> requiredParams) {
            final var validationRules = new ValidationRules(null, false, false, requiredParams, null);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsExist() {
            return Stream.of(
                    Arguments.of(ValidDirectEvent.class, Set.of(QUEUE_PARAM)),
                    Arguments.of(ValidTopicEvent.class, Set.of(EXCHANGE_TYPE_PARAM, EXCHANGE_PARAM, BINDING_KEYS_PARAM)));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsMissing(Class<?> eventClass, Set<String> existingRequiredParams,
                Set<String> missingRequiredParams) {
            final var requiredParams = Stream.concat(existingRequiredParams.stream(), missingRequiredParams.stream())
                    .collect(toSet());
            final var validationRules = new ValidationRules(null, false, false, requiredParams, null);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            final var missingParamException = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            missingRequiredParams.forEach(param -> assertThat(missingParamException.getMessage(), containsString(param)));
            assertThat(missingParamException.getMessage(), containsString("missing in annotation"));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsMissing() {
            return Stream.of(
                    Arguments.of(ValidDirectEvent.class, emptySet(), Set.of("doesNotExist")),
                    Arguments.of(ValidDirectEvent.class, Set.of(QUEUE_PARAM), Set.of("doesNotExist")),
                    Arguments.of(ValidDirectEvent.class, emptySet(), Set.of("doesNotExist", "anotherParamWhichDoesNotExist")),
                    Arguments.of(ValidTopicEvent.class, Set.of(EXCHANGE_TYPE_PARAM, EXCHANGE_PARAM, BINDING_KEYS_PARAM),
                            Set.of("doesNotExists")));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsAreKebabCase(Class<?> eventClass, Set<String> requiredKebabCaseParams) {
            final var validationRules = new ValidationRules(null, false, false, null, requiredKebabCaseParams);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            assertDoesNotThrow(() -> validator.validate(annotationInstance));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsAreKebabCase() {
            return Stream.of(
                    Arguments.of(ValidDirectEvent.class, Set.of(QUEUE_PARAM)),
                    Arguments.of(ValidTopicEvent.class, Set.of(EXCHANGE_PARAM)));
        }

        @ParameterizedTest
        @MethodSource
        public void validateConsumedEventParamsAreNotKebabCase(Class<?> eventClass, Set<String> requiredKebabCaseParams) {
            final var validationRules = new ValidationRules(null, false, false, null, requiredKebabCaseParams);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            requiredKebabCaseParams.forEach(parameter -> assertThat(exception.getMessage(), containsString(parameter)));
            assertThat(exception.getMessage(), endsWith("is not formatted in kebab case."));
        }

        @SuppressWarnings("unused")
        private static Stream<Arguments> validateConsumedEventParamsAreNotKebabCase() {
            return Stream.of(
                    Arguments.of(CamelCaseDirectEvent.class, Set.of(QUEUE_PARAM)),
                    Arguments.of(NonKebabExchangeTopicEvent.class, Set.of(EXCHANGE_PARAM)));
        }

        @ParameterizedTest
        @ValueSource(classes = { DoubleAsteriskBindingKey.class,
                UppercaseBindingKeyTopicEvent.class,
                DotEndingBindingKeyTopicEvent.class })
        public void validateBindingKeysAreInValidFormat(Class<?> eventClass) {
            final var validationRules = new ValidationRules(null, false, true, null, null);
            final var annotationInstance = getAnnotationInstance(CONSUMED_EVENT_ANNOTATION_CLASS, eventClass);
            final var validator = getValidatorService(validationRules, eventClass);

            final var exception = assertThrows(MessageHandlerValidationException.class,
                    () -> validator.validate(annotationInstance));

            assertThat(exception.getMessage(), containsString("Binding key"));
            assertThat(exception.getMessage(), containsString("does not conform to the TOPIC format"));
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

    @ConsumedEvent(queue = "kebab-case-queue")
    public record ValidDirectEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "test.*.key",
            "test.no.wildcard",
            "testsimple", "test.end.with.wildcard.*" })
    public record ValidTopicEvent() {
    }

    @ConsumedEvent(queue = "CamelCaseQueue")
    public record CamelCaseDirectEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "NonKebabExchange", bindingKeys = { "wrong.**.key" })
    public record NonKebabExchangeTopicEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "wrong.**.key" })
    public record DoubleAsteriskBindingKey() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "WRONG.upper.case" })
    public record UppercaseBindingKeyTopicEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "kebab-topic-exchange", bindingKeys = {
            "wrong.end.with.dot." })
    public record DotEndingBindingKeyTopicEvent() {
    }
}