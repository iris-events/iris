package id.global.event.messaging.deployment;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_TYPE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.QUEUE_PARAM;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.event.messaging.deployment.validation.AnnotationInstanceValidator;
import id.global.event.messaging.deployment.validation.ValidationRules;

public class MessageHandlerScanner {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerScanner.class);
    private final DotName DOT_NAME_MESSAGE_HANDLER = DotName.createSimple(MessageHandler.class.getCanonicalName());
    private final DotName DOT_NAME_CONSUMED_EVENT = DotName.createSimple(ConsumedEvent.class.getCanonicalName());
    private final IndexView index;

    public MessageHandlerScanner(IndexView index) {
        this.index = index;
    }

    public List<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations() {

        final var directAnnotations = index.getAnnotations(DOT_NAME_MESSAGE_HANDLER).stream();

        return scanDirectMessageHandlerAnnotations(directAnnotations).collect(Collectors.toList());
    }

    private Stream<MessageHandlerInfoBuildItem> scanDirectMessageHandlerAnnotations(
            Stream<AnnotationInstance> directAnnotations) {
        return directAnnotations.filter(isNotSyntheticPredicate()).map(annotationInstance -> {
            final var validationRules = getValidationRules(Collections.emptySet(), Collections.emptySet());
            final var validator = new AnnotationInstanceValidator(index, validationRules);
            validator.validate(annotationInstance);

            final var methodInfo = annotationInstance.target().asMethod();
            final var methodParameters = methodInfo.parameters();

            final var eventAnnotation = getEventAnnotation(methodParameters, index);
            final var queue = Optional.ofNullable(eventAnnotation.value(QUEUE_PARAM))
                    .map(AnnotationValue::asString)
                    .orElse(null);
            final var exchange = Optional.ofNullable(eventAnnotation.value(EXCHANGE_PARAM))
                    .map(AnnotationValue::asString)
                    .orElse(null);
            final var exchangeType = Optional.ofNullable(eventAnnotation.value(EXCHANGE_TYPE_PARAM))
                    .map(AnnotationValue::asString)
                    .map(ExchangeType::fromType)
                    .orElse(DIRECT);
            final var bindingKeys = Optional.ofNullable(eventAnnotation.value(BINDING_KEYS_PARAM))
                    .map(AnnotationValue::asStringArray)
                    .orElse(null);

            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.name(),
                    queue,
                    exchange,
                    bindingKeys,
                    exchangeType);
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

    private AnnotationInstance getEventAnnotation(final List<Type> parameters,
            final IndexView index) {

        final var consumedEventTypes = parameters.stream()
                .map(Type::name)
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .map(classInfo -> classInfo.classAnnotation(DOT_NAME_CONSUMED_EVENT))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (consumedEventTypes.isEmpty()) {
            throw new IllegalArgumentException("Consumed Event not found");
        }

        if (consumedEventTypes.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple consumed Events detected. Message handler can only handle one event type.");
        }

        return consumedEventTypes.get(0);
    }
}
