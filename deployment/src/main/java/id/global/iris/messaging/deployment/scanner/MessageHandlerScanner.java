package id.global.iris.messaging.deployment.scanner;

import static id.global.common.annotations.iris.ExchangeType.FANOUT;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import id.global.common.annotations.iris.ExchangeType;
import id.global.common.annotations.iris.Message;
import id.global.common.annotations.iris.MessageHandler;
import id.global.iris.amqp.parsers.BindingKeysParser;
import id.global.iris.amqp.parsers.ConsumerPerInstanceParser;
import id.global.iris.amqp.parsers.ConsumerPrefetchCountParser;
import id.global.iris.amqp.parsers.DeadLetterQueueParser;
import id.global.iris.amqp.parsers.ExchangeParser;
import id.global.iris.amqp.parsers.ExchangeTtlParser;
import id.global.iris.amqp.parsers.MessageScopeParser;
import id.global.iris.amqp.parsers.QueueAutoDeleteParser;
import id.global.iris.amqp.parsers.QueueDurableParser;
import id.global.iris.amqp.parsers.RolesAllowedParser;
import id.global.iris.messaging.deployment.MessageHandlerInfoBuildItem;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;
import id.global.iris.messaging.deployment.validation.AnnotationInstanceValidator;

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

                    final var messageAnnotation = getMessageAnnotation(methodInfo, index);
                    annotationValidator.validate(messageAnnotation);

                    final ExchangeType exchangeType = ExchangeType
                            .valueOf(messageAnnotation.valueWithDefault(index, AnnotationInstanceParams.EXCHANGE_TYPE_PARAM)
                                    .asString());

                    final var exchange = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
                    final var scope = MessageScopeParser.getFromAnnotationInstance(messageAnnotation, index);
                    final var ttl = ExchangeTtlParser.getFromAnnotationInstance(messageAnnotation, index);
                    final var deadLetter = DeadLetterQueueParser.getFromAnnotationInstance(messageAnnotation, index);

                    final var bindingKeys = exchangeType != FANOUT
                            ? BindingKeysParser.getFromAnnotationInstance(messageHandlerAnnotation, messageAnnotation)
                            : null;
                    final var durable = QueueDurableParser.getFromAnnotationInstance(messageHandlerAnnotation, index);
                    final var autoDelete = QueueAutoDeleteParser.getFromAnnotationInstance(messageHandlerAnnotation, index);
                    final var perInstance = ConsumerPerInstanceParser.getFromAnnotationInstance(messageHandlerAnnotation,
                            index);
                    final var prefetchCount = ConsumerPrefetchCountParser.getFromAnnotationInstance(messageHandlerAnnotation,
                            index);
                    final var handlerRolesAllowed = RolesAllowedParser.getFromAnnotationInstance(messageHandlerAnnotation,
                            index);

                    return new MessageHandlerInfoBuildItem(
                            methodInfo.declaringClass(),
                            methodInfo.parameters().get(0),
                            methodInfo.returnType(),
                            methodInfo.name(),
                            exchange,
                            exchangeType,
                            bindingKeys,
                            scope,
                            durable,
                            autoDelete,
                            perInstance,
                            prefetchCount,
                            ttl,
                            deadLetter,
                            handlerRolesAllowed);
                });
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private AnnotationInstanceValidator getAnnotationValidator() {
        return new AnnotationInstanceValidator(index);
    }

    public static AnnotationInstance getMessageAnnotation(final MethodInfo methodInfo, final IndexView index) {
        final var parameters = methodInfo.parameters();
        final var consumedEventTypes = parameters.stream()
                .map(Type::name)
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .map(classInfo -> classInfo.classAnnotation(DOT_NAME_MESSAGE))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (consumedEventTypes.isEmpty()) {
            throw new IllegalArgumentException(String.format("Consumed Event not found for method %s in class %s.",
                    methodInfo.name(), methodInfo.declaringClass()));
        }

        if (consumedEventTypes.size() > 1) {
            throw new IllegalArgumentException(String.format(
                    "Multiple consumed Events detected for method %s in class %s. Message handler can only handle one event type.",
                    methodInfo.name(), methodInfo.declaringClass()));
        }

        return consumedEventTypes.get(0);
    }
}
