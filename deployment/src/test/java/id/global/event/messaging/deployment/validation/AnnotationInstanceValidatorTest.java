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
                false,
                Set.of(QUEUE_PARAM),
                Set.of(QUEUE_PARAM));
        final var validator = getAnnotationInstanceValidator(serviceClass,
                validationRules);

        assertDoesNotThrow(() -> validator.validate(annotationInstance));
    }

    @Test
    public void validateNonKebabCaseQueueShouldFail() {
        final var serviceClass = ValidationNotOkTestService.class;
        final var annotationClass = MessageHandler.class;

        final var annotationInstance = getAnnotationInstance(serviceClass, annotationClass);
        final var validationRules = new ValidationRules(null, false, false, null, Set.of(QUEUE_PARAM));
        final var validator = getAnnotationInstanceValidator(
                serviceClass, validationRules);

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
        final var serviceClass = ValidationOKTestService.class;
        final var annotationClass = MessageHandler.class;

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
        final var validationOkServiceClass = ValidationOKTestService.class;
        final var messageHandlerAnnotationClass = MessageHandler.class;
        final var topicMessageHandlerAnnotationClass = TopicMessageHandler.class;
        final var validationNonKebabServiceClass = ValidationNotOkTestService.class;

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

        final var topicValidationRules = new ValidationRules(null, false, false, null, Set.of(EXCHANGE_PARAM));
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

    @Test
    public void validateTopicParameter() {
        final var validationRules = new ValidationRules(null, false, true, null, null);
        final var serviceClassOk = ValidationOKTestService.class;
        final var serviceClassNotOk = ValidationNotOkTestService.class;
        final var annotationClass = TopicMessageHandler.class;

        AnnotationInstanceValidator validatorOk = getAnnotationInstanceValidator(serviceClassOk,
                validationRules);
        AnnotationInstanceValidator validatorNotOk = getAnnotationInstanceValidator(serviceClassNotOk,
                validationRules);

        assertDoesNotThrow(() -> validatorOk.validateTopicValidity(getAnnotationInstance(serviceClassOk, annotationClass)));
        assertThrows(MessageHandlerValidationException.class,
                () -> validatorNotOk.validateTopicValidity(getAnnotationInstance(serviceClassNotOk, annotationClass)));

        assertThrows(MessageHandlerValidationException.class,
                () -> validatorNotOk.validateTopicValidity(getAnnotationInstance(serviceClassNotOk, annotationClass, 1)));
        assertThrows(MessageHandlerValidationException.class,
                () -> validatorNotOk.validateTopicValidity(getAnnotationInstance(serviceClassNotOk, annotationClass, 2)));
    }

    private AnnotationInstanceValidator getAnnotationInstanceValidator(Class<?> serviceClass,
            ValidationRules validationRules) {
        return getAnnotationInstanceValidator(indexOf(serviceClass, Event.class), validationRules);
    }

    private AnnotationInstanceValidator getAnnotationInstanceValidator(Index index, ValidationRules validationRules) {
        return new AnnotationInstanceValidator(index, validationRules);
    }

    private AnnotationInstance getAnnotationInstance(Class<?> serviceClass, Class<?> annotationClass) {
        return getAnnotationInstance(serviceClass, annotationClass, 0);
    }

    private AnnotationInstance getAnnotationInstance(Class<?> serviceClass, Class<?> annotationClass, int index) {
        return indexOf(serviceClass, Event.class)
                .getAnnotations(DotName.createSimple(annotationClass.getCanonicalName()))
                .get(index);
    }

    private static class ValidationOKTestService {

        @MessageHandler(queue = "kebab-case-queue")
        public void handle(Event event) {
        }

        @TopicMessageHandler(exchange = "kebab-topic-exchange", bindingKeys = { "test.*.key", "test.no.wildcard",
                "testsimple", "test.end.with.wildcard.*" })
        public void handleTopic(Event event) {
        }

    }

    private static class ValidationNotOkTestService {

        @MessageHandler(queue = "CamelCaseQueue")
        public void handle(Event event) {
        }

        @TopicMessageHandler(exchange = "NonKebabExchange", bindingKeys = { "wrong.**.key" })
        public void handleTopic(Event event) {
        }

        @TopicMessageHandler(exchange = "NonKebabExchange", bindingKeys = { "WRONG.upper.case" })
        public void handleTopic2(Event event) {
        }

        @TopicMessageHandler(exchange = "NonKebabExchange", bindingKeys = { "WRONG.end.with.dot." })
        public void handleTopic3(Event event) {
        }

    }
}