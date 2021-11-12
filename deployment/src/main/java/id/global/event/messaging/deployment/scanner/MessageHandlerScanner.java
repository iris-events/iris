package id.global.event.messaging.deployment.scanner;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_TYPE_PARAM;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import id.global.event.messaging.deployment.MessageHandlerValidationException;
import id.global.event.messaging.deployment.validation.AnnotationInstanceValidator;
import id.global.event.messaging.deployment.validation.ValidationRules;
import io.smallrye.asyncapi.runtime.util.GidAnnotationParser;

public class MessageHandlerScanner {
    private final DotName DOT_NAME_MESSAGE_HANDLER = DotName.createSimple(MessageHandler.class.getCanonicalName());
    private final DotName DOT_NAME_CONSUMED_EVENT = DotName.createSimple(ConsumedEvent.class.getCanonicalName());
    private final IndexView index;
    private final String appId;

    public MessageHandlerScanner(IndexView index, String appId) {
        this.index = index;
        this.appId = appId;
    }

    public List<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations() {
        final var methodAnnotations = index.getAnnotations(DOT_NAME_MESSAGE_HANDLER).stream();

        return scanMessageHandlerAnnotations(methodAnnotations).collect(Collectors.toList());
    }

    private Stream<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations(Stream<AnnotationInstance> directAnnotations) {
        final AnnotationInstanceValidator annotationValidator = getAnnotationValidator();

        return directAnnotations.filter(isNotSyntheticPredicate()).map(methodAnnotation -> {
            annotationValidator.validate(methodAnnotation);
            final var methodInfo = methodAnnotation.target().asMethod();
            final var methodParameters = methodInfo.parameters();

            final var eventAnnotation = getEventAnnotation(methodParameters, index);
            annotationValidator.validate(eventAnnotation);

            final var exchange = getExchangeOrDefault(eventAnnotation.value(EXCHANGE_PARAM), appId);

            final var exchangeType = Optional.ofNullable(eventAnnotation.value(EXCHANGE_TYPE_PARAM))
                    .map(AnnotationValue::asString)
                    .map(ExchangeType::fromType)
                    .orElse(DIRECT);

            final var bindingKeys = getBindingKeysOrDefault(eventAnnotation);

            return new MessageHandlerInfoBuildItem(
                    methodInfo.declaringClass(),
                    methodInfo.parameters().get(0),
                    methodInfo.returnType(),
                    methodInfo.name(),
                    exchange,
                    bindingKeys,
                    exchangeType);
        });
    }

    private String[] getBindingKeysOrDefault(AnnotationInstance eventAnnotation) {
        return Optional.ofNullable(eventAnnotation.value(BINDING_KEYS_PARAM))
                .map(AnnotationValue::asStringArray)
                .orElseGet(() -> getDefaultBindingKeys(eventAnnotation));
    }

    private String[] getDefaultBindingKeys(AnnotationInstance eventAnnotation) {
        ExchangeType exchangeType = ExchangeType.fromType(eventAnnotation.value(EXCHANGE_TYPE_PARAM).asEnum());
        if (exchangeType.equals(ExchangeType.TOPIC)) {
            throw new MessageHandlerValidationException("TOPIC event type must have bindingKeys set.");
        }
        if (exchangeType.equals(FANOUT)) {
            return null;
        }
        return new String[] {
                GidAnnotationParser.camelToKebabCase(eventAnnotation.target().asClass().simpleName()) };
    }

    private String getExchangeOrDefault(AnnotationValue annotationValue, String appId) {
        return Optional.ofNullable(annotationValue.asString()).orElseGet(() -> GidAnnotationParser.camelToKebabCase(appId));
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private AnnotationInstanceValidator getAnnotationValidator() {
        return new AnnotationInstanceValidator(index, getValidationRules());
    }

    private ValidationRules getValidationRules() {
        return new ValidationRules(1, false);
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
