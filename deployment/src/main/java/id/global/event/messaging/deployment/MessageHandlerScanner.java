package id.global.event.messaging.deployment;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.asyncapi.spec.enums.ExchangeType.FANOUT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.QUEUE_PARAM;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.deployment.validation.AnnotationInstanceValidator;
import id.global.event.messaging.deployment.validation.ValidationRules;

public class MessageHandlerScanner {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerScanner.class);
    private final IndexView index;

    public MessageHandlerScanner(IndexView index) {
        this.index = index;
    }

    public List<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations() {
        final var messageHandlerDotName = DotName.createSimple(MessageHandler.class.getCanonicalName());
        final var fanoutMessageHandlerDotName = DotName.createSimple(FanoutMessageHandler.class.getCanonicalName());
        final var topicMessageHandlerDotName = DotName.createSimple(TopicMessageHandler.class.getCanonicalName());

        final var directAnnotations = index.getAnnotations(messageHandlerDotName).stream();
        final var fanoutAnnotations = index.getAnnotations(fanoutMessageHandlerDotName).stream();
        final var topicAnnotations = index.getAnnotations(topicMessageHandlerDotName).stream();

        return Stream
                .concat(Stream.concat(scanDirectMessageHandlerAnnotations(directAnnotations),
                        scanFanoutMessageHandlerAnnotations(fanoutAnnotations)),
                        scanTopicMessageHandlerAnnotations(topicAnnotations))
                .collect(Collectors.toList());
    }

    private Stream<MessageHandlerInfoBuildItem> scanTopicMessageHandlerAnnotations(
            Stream<AnnotationInstance> topicAnnotations) {
        return topicAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {
            final var validationRules = getValidationRules(true, Set.of(EXCHANGE_PARAM, BINDING_KEYS_PARAM),
                    Set.of(EXCHANGE_PARAM));
            final var validator = new AnnotationInstanceValidator(index, validationRules);
            validator.validate(annotationInstance);

            final var methodInfo = annotationInstance.target().asMethod();
            final var exchange = annotationInstance.value(EXCHANGE_PARAM).asString();
            final var bindingKeys = annotationInstance.value(BINDING_KEYS_PARAM).asStringArray();
            // Implement parsing other parameters if needed here

            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    null,
                    exchange,
                    bindingKeys,
                    TOPIC);
        });
    }

    private Stream<MessageHandlerInfoBuildItem> scanFanoutMessageHandlerAnnotations(
            Stream<AnnotationInstance> fanoutAnnotations) {
        return fanoutAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {
            final var validationRules = getValidationRules(Set.of(EXCHANGE_PARAM), Set.of(EXCHANGE_PARAM));
            final var validator = new AnnotationInstanceValidator(index, validationRules);
            validator.validate(annotationInstance);

            final var methodInfo = annotationInstance.target().asMethod();
            final var exchange = annotationInstance.value(EXCHANGE_PARAM).asString();
            // Implement parsing other parameters if needed here

            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    null,
                    exchange,
                    null,
                    FANOUT);
        });
    }

    private Stream<MessageHandlerInfoBuildItem> scanDirectMessageHandlerAnnotations(
            Stream<AnnotationInstance> directAnnotations) {
        return directAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {
            final var validationRules = getValidationRules(Set.of(QUEUE_PARAM), Set.of(QUEUE_PARAM));
            final var validator = new AnnotationInstanceValidator(index, validationRules);
            validator.validate(annotationInstance);

            final var exchange = annotationInstance.value(EXCHANGE_PARAM) != null
                    ? annotationInstance.value(EXCHANGE_PARAM).asString()
                    : null;
            final var queue = annotationInstance.value(QUEUE_PARAM).asString();
            // Implement parsing other parameters if needed here

            final var methodInfo = annotationInstance.target().asMethod();
            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    queue,
                    exchange,
                    null,
                    DIRECT);
        });
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private ValidationRules getValidationRules(final Set<String> requiredParams, final Set<String> kebabCaseOnlyParams) {
        return getValidationRules(false, requiredParams, kebabCaseOnlyParams);
    }

    private ValidationRules getValidationRules(boolean checkTopicValidity, final Set<String> requiredParams,
            final Set<String> kebabCaseOnlyParams) {
        return new ValidationRules(1,
                false,
                checkTopicValidity,
                requiredParams,
                kebabCaseOnlyParams);

    }
}
