package id.global.event.messaging.deployment.scanner;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_TYPE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.ROUTING_KEY;
import static java.util.Collections.emptySet;

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

import id.global.common.annotations.amqp.ConsumedEvent;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.event.messaging.deployment.MessageHandlerInfoBuildItem;
import id.global.event.messaging.deployment.validation.AnnotationInstanceValidator;
import id.global.event.messaging.deployment.validation.ValidationRules;

public class MessageHandlerScanner {
    private final DotName DOT_NAME_MESSAGE_HANDLER = DotName.createSimple(MessageHandler.class.getCanonicalName());
    private final DotName DOT_NAME_CONSUMED_EVENT = DotName.createSimple(ConsumedEvent.class.getCanonicalName());
    private final IndexView index;

    public MessageHandlerScanner(IndexView index) {
        this.index = index;
    }

    public List<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations() {
        final var methodAnnotations = index.getAnnotations(DOT_NAME_MESSAGE_HANDLER).stream();

        return scanMessageHandlerAnnotations(methodAnnotations).collect(Collectors.toList());
    }

    private Stream<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations(Stream<AnnotationInstance> directAnnotations) {

        final AnnotationInstanceValidator messageHandlerValidator = getMessageHandlerValidator();
        final AnnotationInstanceValidator eventValidator = getEventValidator();

        return directAnnotations.filter(isNotSyntheticPredicate()).map(methodAnnotation -> {
            messageHandlerValidator.validate(methodAnnotation);
            final var methodInfo = methodAnnotation.target().asMethod();
            final var methodParameters = methodInfo.parameters();

            final var eventAnnotation = getEventAnnotation(methodParameters, index);
            eventValidator.validate(eventAnnotation);

            final var routingKey = Optional.ofNullable(eventAnnotation.value(ROUTING_KEY))
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
                    methodInfo.returnType(),
                    methodInfo.name(),
                    routingKey,
                    exchange,
                    bindingKeys,
                    exchangeType);
        });
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private AnnotationInstanceValidator getEventValidator() {
        final var eventValidationRules = getValidationRules(Set.of(EXCHANGE_PARAM, EXCHANGE_TYPE_PARAM),
                Set.of(EXCHANGE_PARAM, ROUTING_KEY));
        return new AnnotationInstanceValidator(index, eventValidationRules);
    }

    private AnnotationInstanceValidator getMessageHandlerValidator() {
        final var methodValidationRules = getValidationRules(emptySet(), emptySet());
        return new AnnotationInstanceValidator(index, methodValidationRules);
    }

    private ValidationRules getValidationRules(final Set<String> requiredParams, final Set<String> kebabCaseOnlyParams) {
        return new ValidationRules(1,
                false,
                requiredParams,
                kebabCaseOnlyParams);

    }

    private AnnotationInstance getEventAnnotation(final List<Type> parameters, final IndexView index) {

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
