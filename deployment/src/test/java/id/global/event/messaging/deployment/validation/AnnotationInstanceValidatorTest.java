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

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.BaseIndexingTest;
import id.global.event.messaging.deployment.MessageHandlerValidationException;
import id.global.event.messaging.test.Event;

class AnnotationInstanceValidatorTest extends BaseIndexingTest {

    @Test
    void validateWithRules() {
        final var serviceClass = ValidationOKTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(
                1,
                false,
                Set.of(QUEUE_PARAM),
                Set.of(QUEUE_PARAM));
        final var validator = getAnnotationInstanceValidator(serviceClass,
                validationRules);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @Test
    public void validateNonKebabCaseQueueShouldFail() {
        final var serviceClass = ValidationNotKebabCaseTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(null, null, null, Set.of(QUEUE_PARAM));
        final var validator = getAnnotationInstanceValidator(
                serviceClass, validationRules);

        assertThrows(MessageHandlerValidationException.class, () -> validator.validateParamsAreKebabCase(annotationInstance));
    }

    @Test
    public void validateMethodParamCount() {
        final var serviceClass = ValidationOKTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(1, null, null, null);
        final var validator = getAnnotationInstanceValidator(
                serviceClass, validationRules);
        final var wrongValidationRules = new ValidationRules(3, null, null, null);
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
        final var validationRules = new ValidationRules(null, false, null, null);
        final var indexWithEventAsExternalDependency = indexOf(serviceClass);
        final var validator = getAnnotationInstanceValidator(
                indexWithEventAsExternalDependency, validationRules);

        assertThrows(MessageHandlerValidationException.class,
                () -> validator.validateMethodParamExternalDependency(annotationInstance));
    }

    @Test
    public void validateParamsExist() {
        final var serviceClass = ValidationOKTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(null, null, Set.of(QUEUE_PARAM), null);
        AnnotationInstanceValidator validator = getAnnotationInstanceValidator(
                serviceClass, validationRules);

        assertDoesNotThrow(() -> validator.validateParamsExist(annotationInstance));

        String nonExistentParam = "doesntExist";
        final var nonExistingParamRules = new ValidationRules(null, null, Set.of(nonExistentParam), null);
        final var nonExistingParamValidator = getAnnotationInstanceValidator(
                serviceClass, nonExistingParamRules);

        assertThrows(MessageHandlerValidationException.class,
                () -> nonExistingParamValidator.validateParamsExist(annotationInstance));

        final var additionalNonExistingParamRules = new ValidationRules(null, null, Set.of(QUEUE_PARAM, nonExistentParam),
                null);
        final var additionalNonExistingParamValidator = getAnnotationInstanceValidator(
                serviceClass, additionalNonExistingParamRules);

        assertThrows(MessageHandlerValidationException.class,
                () -> additionalNonExistingParamValidator.validateParamsExist(annotationInstance));
    }

    @Test
    public void validateParamsAreKebabCase() {
        final var validationRules = new ValidationRules(null, null, null, Set.of(QUEUE_PARAM));
        final var validationOkServiceClass = ValidationOKTestService.class;
        final var messageHandlerAnnotationClass = MessageHandler.class;
        final var topicMessageHandlerAnnotationClass = TopicMessageHandler.class;
        final var validationNonKebabServiceClass = ValidationNotKebabCaseTestService.class;

        final var validationOkMessageHandlerInstance = getAnnotationInstance(validationOkServiceClass,
                messageHandlerAnnotationClass);
        final var validationOkTopicMessageHandlerInstance = getAnnotationInstance(validationOkServiceClass,
                topicMessageHandlerAnnotationClass);
        final var validationNonKebabMessageHandlerInstance = getAnnotationInstance(validationNonKebabServiceClass,
                messageHandlerAnnotationClass);
        final var validationNonKebabTopicMessageHandlerInstance = getAnnotationInstance(validationNonKebabServiceClass,
                topicMessageHandlerAnnotationClass);

        final var validator = getAnnotationInstanceValidator(
                validationOkServiceClass, validationRules);

        final var topicValidationRules = new ValidationRules(null, null, null, Set.of(EXCHANGE_PARAM));
        AnnotationInstanceValidator topicValidator = getAnnotationInstanceValidator(
                validationOkServiceClass, topicValidationRules);

        AnnotationInstanceValidator nonKebabMessageHandlerValidator = getAnnotationInstanceValidator(
                validationNonKebabServiceClass, validationRules);

        AnnotationInstanceValidator nonKebabTopicMessageHandlerValidator = getAnnotationInstanceValidator(
                validationNonKebabServiceClass, topicValidationRules);

        assertDoesNotThrow(() -> validator.validateParamsAreKebabCase(validationOkMessageHandlerInstance));
        assertDoesNotThrow(() -> topicValidator.validateParamsAreKebabCase(validationOkTopicMessageHandlerInstance));
        assertThrows(MessageHandlerValidationException.class,
                () -> nonKebabMessageHandlerValidator.validateParamsAreKebabCase(validationNonKebabMessageHandlerInstance));
        assertThrows(MessageHandlerValidationException.class,
                () -> nonKebabTopicMessageHandlerValidator.validateParamsAreKebabCase(
                        validationNonKebabTopicMessageHandlerInstance));
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

        @MessageHandler(queue = "kebab-case-queue")
        public void handle(Event event) {
        }

        @TopicMessageHandler(exchange = "kebab-topic-exchange", bindingKeys = "test.*.key")
        public void handleTopic(Event event) {
        }
    }

    private static class ValidationNotKebabCaseTestService {

        @MessageHandler(queue = "CamelCaseQueue")
        public void handle(Event event) {
        }

        @TopicMessageHandler(exchange = "NonKebabExchange", bindingKeys = "test.*.key")
        public void handleTopic(Event event) {
        }
    }
}