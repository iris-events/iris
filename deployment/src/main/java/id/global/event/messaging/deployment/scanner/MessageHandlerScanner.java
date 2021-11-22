package id.global.event.messaging.deployment.scanner;

import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_TYPE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.ROUTING_KEY_PARAM;

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

import id.global.asyncapi.runtime.util.GidAnnotationParser;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.annotations.amqp.Scope;
import id.global.event.messaging.deployment.MessageHandlerInfoBuildItem;
import id.global.event.messaging.deployment.validation.AnnotationInstanceValidator;

public class MessageHandlerScanner {
    private static final DotName DOT_NAME_MESSAGE_HANDLER = DotName.createSimple(MessageHandler.class.getCanonicalName());
    private static final DotName DOT_NAME_MESSAGE = DotName.createSimple(Message.class.getCanonicalName());
    private final IndexView index;

    public MessageHandlerScanner(IndexView index) {
        this.index = index;
    }

    public List<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations() {
        final var methodAnnotations = index.getAnnotations(DOT_NAME_MESSAGE_HANDLER).stream();

        return scanMessageHandlerAnnotations(methodAnnotations).collect(Collectors.toList());
    }

    private Stream<MessageHandlerInfoBuildItem> scanMessageHandlerAnnotations(Stream<AnnotationInstance> directAnnotations) {
        final AnnotationInstanceValidator annotationValidator = getAnnotationValidator();

        return directAnnotations.filter(isNotSyntheticPredicate())
                .map(messageHandlerAnnotation -> {

                    annotationValidator.validate(messageHandlerAnnotation);
                    final var methodInfo = messageHandlerAnnotation.target().asMethod();
                    final var methodParameters = methodInfo.parameters();

                    final var messageAnnotation = getMessageAnnotation(methodParameters, index);
                    annotationValidator.validate(messageAnnotation);

                    final ExchangeType exchangeType = ExchangeType
                            .valueOf(messageAnnotation.valueWithDefault(index, EXCHANGE_TYPE_PARAM)
                                    .asString());
                    String name = messageAnnotation.value("name").asString();
                    if (name == null || name.isEmpty()) {
                        name = getMessageClassKebabCase(messageAnnotation);
                    }

                    final var bindingKeys = getBindingKeysOrDefault(messageHandlerAnnotation, messageAnnotation, exchangeType);
                    final Scope scope = Scope.valueOf(messageAnnotation.valueWithDefault(index, "scope").asString());
                    final long ttl = messageAnnotation.valueWithDefault(index, "ttl").asLong();
                    final String deadLetter = messageAnnotation.valueWithDefault(index, "deadLetter").asString();

                    final boolean durable = messageHandlerAnnotation.valueWithDefault(index, "durable").asBoolean();
                    final boolean autoDelete = messageHandlerAnnotation.valueWithDefault(index, "autoDelete").asBoolean();
                    final boolean perInstance = messageHandlerAnnotation.valueWithDefault(index, "perInstance").asBoolean();
                    final int prefetchCount = messageHandlerAnnotation.valueWithDefault(index, "prefetchCount").asInt();

                    return new MessageHandlerInfoBuildItem(
                            methodInfo.declaringClass(),
                            methodInfo.parameters().get(0),
                            methodInfo.returnType(),
                            methodInfo.name(),
                            name,
                            exchangeType,
                            bindingKeys,
                            scope,
                            durable,
                            autoDelete,
                            perInstance,
                            prefetchCount,
                            ttl,
                            deadLetter);
                });
    }

    // TODO: extract all annotation value retrievals to common place: MessageHandlerScanner, AmqpProducer and smallrye-eda asyncapi generator should all use same defaults retrieval logic
    private String[] getBindingKeysOrDefault(AnnotationInstance messageHandlerAnnotation, AnnotationInstance messageAnnotation,
            ExchangeType exchangeType) {
        return Optional.ofNullable(messageHandlerAnnotation.value(BINDING_KEYS_PARAM))
                .map(AnnotationValue::asStringArray)
                .orElseGet(() -> getDefaultBindingKeys(messageAnnotation, exchangeType));
    }

    private String[] getDefaultBindingKeys(AnnotationInstance messageAnnotation, ExchangeType exchangeType) {
        if (exchangeType.equals(FANOUT)) {
            return null;
        }

        return Optional.ofNullable(messageAnnotation.value(ROUTING_KEY_PARAM))
                .map(AnnotationValue::asString)
                .map(s -> new String[] { s })
                .orElseGet(() -> new String[] { getMessageClassKebabCase(messageAnnotation) });

    }

    private String getMessageClassKebabCase(final AnnotationInstance messageAnnotation) {
        return GidAnnotationParser.camelToKebabCase(messageAnnotation.target().asClass().simpleName());
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private AnnotationInstanceValidator getAnnotationValidator() {
        return new AnnotationInstanceValidator(index);
    }

    @Deprecated(forRemoval = true, since = "this is wrongly handling default")
    public static ExchangeType getExchangeType(AnnotationInstance annotationInstance) {
        // TODO: change extraction to common defaulting code
        return Optional.ofNullable(annotationInstance.value(EXCHANGE_TYPE_PARAM))
                .map(AnnotationValue::asString)
                .map(ExchangeType::fromType)
                .orElse(ExchangeType.FANOUT);
    }

    public static AnnotationInstance getMessageAnnotation(final List<Type> parameters, final IndexView index) {

        final var consumedEventTypes = parameters.stream()
                .map(Type::name)
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .map(classInfo -> classInfo.classAnnotation(DOT_NAME_MESSAGE))
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
