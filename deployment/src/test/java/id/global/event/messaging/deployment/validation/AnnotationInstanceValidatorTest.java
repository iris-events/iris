package id.global.event.messaging.deployment.validation;

import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.QUEUE_PARAM;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.event.messaging.BaseIndexingTest;
import id.global.event.messaging.deployment.MessageHandlerValidationException;
import id.global.event.messaging.test.Event;

class AnnotationInstanceValidatorTest extends BaseIndexingTest {

    @Test
    void validateWithRules() {
        final var eventClass = ValidDirectEvent.class;
        final var annotationClass = ConsumedEvent.class;

        final var annotationInstance = getAnnotationInstance(eventClass, annotationClass);
        final var validationRules = new ValidationRules(
                1,
                false,
                false,
                Set.of(QUEUE_PARAM),
                Set.of(QUEUE_PARAM));
        final var validator = getAnnotationInstanceValidator(eventClass,
                validationRules);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @Test
    public void validateNonKebabCaseQueueShouldFail() {
        final var eventClass = CameCaseDirectEvent.class;
        final var annotationClass = ConsumedEvent.class;

        final var annotationInstance = getAnnotationInstance(eventClass, annotationClass);
        final var validationRules = new ValidationRules(null, false, false, null, Set.of(QUEUE_PARAM));
        final var validator = getAnnotationInstanceValidator(
                eventClass, validationRules);

        assertThrows(MessageHandlerValidationException.class, () -> validator.validateParamsAreKebabCase(annotationInstance));
    }

    @Test
    public void validateMethodParamCount() {
        final var serviceClass = ValidationOKTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(1, false, false, null, null);
        final var validator = getAnnotationInstanceValidator(
                serviceClass, validationRules);
        final var wrongValidationRules = new ValidationRules(3, false, false, null, null);
        final var wrongValidator = getAnnotationInstanceValidator(
                serviceClass, wrongValidationRules);

        assertDoesNotThrow(() -> validator.validateMethodParamCount(annotationInstance));
        assertThrows(MessageHandlerValidationException.class,
                () -> wrongValidator.validateMethodParamCount(annotationInstance));
    }

    @Test
    public void validateMethodParamExternalDependency() {
        final var serviceClass = ValidationOKTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(null, false, false, null, null);
        final var indexWithEventAsExternalDependency = indexOf(serviceClass);
        final var validator = getAnnotationInstanceValidator(
                indexWithEventAsExternalDependency, validationRules);

        assertThrows(MessageHandlerValidationException.class,
                () -> validator.validateMethodParamExternalDependency(annotationInstance));
    }

    @Test
    public void validateParamsExist() {
        final var serviceClass = ValidDirectEvent.class;
        final var annotationClass = ConsumedEvent.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(null, false, false, Set.of(QUEUE_PARAM), null);
        AnnotationInstanceValidator validator = getAnnotationInstanceValidator(
                serviceClass, validationRules);

        assertDoesNotThrow(() -> validator.validateParamsExist(annotationInstance));

        String nonExistentParam = "doesntExist";
        final var nonExistingParamRules = new ValidationRules(null, false, false, Set.of(nonExistentParam), null);
        final var nonExistingParamValidator = getAnnotationInstanceValidator(
                serviceClass, nonExistingParamRules);

        assertThrows(MessageHandlerValidationException.class,
                () -> nonExistingParamValidator.validateParamsExist(annotationInstance));

        final var additionalNonExistingParamRules = new ValidationRules(null, false, false,
                Set.of(QUEUE_PARAM, nonExistentParam),
                null);
        final var additionalNonExistingParamValidator = getAnnotationInstanceValidator(
                serviceClass, additionalNonExistingParamRules);

        assertThrows(MessageHandlerValidationException.class,
                () -> additionalNonExistingParamValidator.validateParamsExist(annotationInstance));
    }

    @Test
    public void validateParamsAreKebabCase() {
        final var validationRules = new ValidationRules(null, false, false, null, Set.of(QUEUE_PARAM));
        final var consumedEventAnnotationClass = ConsumedEvent.class;
        final var validationOkEventClass = ValidDirectEvent.class;
        final var validationNonKebabEventClass = CameCaseDirectEvent.class;
        final var validationOkTopicEventClass = ValidTopicEvent.class;
        final var validationNonKebabTopicEventClass = NonKebabExchangeTopicEvent.class;

        final var validationOkMessageHandlerInstance = getAnnotationInstance(validationOkEventClass,
                consumedEventAnnotationClass);
        final var validationOkTopicMessageHandlerInstance = getAnnotationInstance(validationOkTopicEventClass,
                consumedEventAnnotationClass);
        final var validationNonKebabMessageHandlerInstance = getAnnotationInstance(validationNonKebabEventClass,
                consumedEventAnnotationClass);
        final var validationNonKebabTopicMessageHandlerInstance = getAnnotationInstance(validationNonKebabTopicEventClass,
                consumedEventAnnotationClass);

        final var validator = getAnnotationInstanceValidator(
                validationOkEventClass, validationRules);

        final var topicValidationRules = new ValidationRules(null, false, false, null, Set.of(EXCHANGE_PARAM));
        AnnotationInstanceValidator topicValidator = getAnnotationInstanceValidator(
                validationOkEventClass, topicValidationRules);

        AnnotationInstanceValidator nonKebabMessageHandlerValidator = getAnnotationInstanceValidator(
                validationNonKebabEventClass, validationRules);

        AnnotationInstanceValidator nonKebabTopicMessageHandlerValidator = getAnnotationInstanceValidator(
                validationNonKebabEventClass, topicValidationRules);

        assertDoesNotThrow(() -> validator.validateParamsAreKebabCase(validationOkMessageHandlerInstance));
        assertDoesNotThrow(() -> topicValidator.validateParamsAreKebabCase(validationOkTopicMessageHandlerInstance));
        assertThrows(MessageHandlerValidationException.class,
                () -> nonKebabMessageHandlerValidator.validateParamsAreKebabCase(validationNonKebabMessageHandlerInstance));
        assertThrows(MessageHandlerValidationException.class,
                () -> nonKebabTopicMessageHandlerValidator.validateParamsAreKebabCase(
                        validationNonKebabTopicMessageHandlerInstance));
    }

    @Test
    public void validateTopicParameter() {
        final var validationRules = new ValidationRules(null, false, true, null, null);
        final var topicEventClassOk = ValidTopicEvent.class;
        final var topicEventClassNotOk = NonKebabExchangeTopicEvent.class;
        final var uppercaseBindingKeyTopicEventClass = UppercaseBindingKeyTopicEvent.class;
        final var dotEndingBindingKeyTopicEventClass = DotEndingBindingKeyTopicEvent.class;
        final var annotationClass = ConsumedEvent.class;

        AnnotationInstanceValidator validatorOk = getAnnotationInstanceValidator(topicEventClassOk,
                validationRules);
        AnnotationInstanceValidator validatorNotOk = getAnnotationInstanceValidator(topicEventClassNotOk,
                validationRules);

        assertDoesNotThrow(() -> validatorOk.validateTopicValidity(getAnnotationInstance(topicEventClassOk, annotationClass)));
        assertThrows(MessageHandlerValidationException.class,
                () -> validatorNotOk.validateTopicValidity(getAnnotationInstance(topicEventClassNotOk, annotationClass)));

        assertThrows(MessageHandlerValidationException.class,
                () -> validatorNotOk
                        .validateTopicValidity(getAnnotationInstance(uppercaseBindingKeyTopicEventClass, annotationClass)));
        assertThrows(MessageHandlerValidationException.class,
                () -> validatorNotOk
                        .validateTopicValidity(getAnnotationInstance(dotEndingBindingKeyTopicEventClass, annotationClass)));
    }

    private AnnotationInstanceValidator getAnnotationInstanceValidator(Class<?> serviceClass,
            ValidationRules validationRules) {
        return getAnnotationInstanceValidator(indexOf(serviceClass, Event.class), validationRules);
    }

    private AnnotationInstanceValidator getAnnotationInstanceValidator(Index index, ValidationRules validationRules) {
        return new AnnotationInstanceValidator(index, validationRules);
    }

    private AnnotationInstance getAnnotationInstance(Class<?> serviceClass, Class<?> annotationClass) {
        return indexOf(serviceClass, Event.class)
                .getAnnotations(DotName.createSimple(annotationClass.getCanonicalName()))
                .get(0);
    }

    private static class ValidationOKTestService {

        @MessageHandler
        public void handle(ValidDirectEvent event) {
        }

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
    public record CameCaseDirectEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "NonKebabExchange", bindingKeys = { "wrong.**.key" })
    public record NonKebabExchangeTopicEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "kebab-topic-exchange", bindingKeys = { "WRONG.upper.case" })
    public record UppercaseBindingKeyTopicEvent() {
    }

    @ConsumedEvent(exchangeType = ExchangeType.TOPIC, exchange = "kebab-topic-exchange", bindingKeys = {
            "wrong.end.with.dot." })
    public record DotEndingBindingKeyTopicEvent() {
    }
}