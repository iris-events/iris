package id.global.event.messaging.deployment.scanner;

import static id.global.common.annotations.amqp.ExchangeType.FANOUT;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import id.global.amqp.BindingKeysParser;
import id.global.amqp.ExchangeParser;
import id.global.amqp.BindingKeysParser;
import id.global.amqp.ConsumerPerInstanceParser;
import id.global.amqp.ConsumerPrefetchCountParser;
import id.global.amqp.DeadLetterQueueParser;
import id.global.amqp.ExchangeParser;
import id.global.amqp.ExchangeTtlParser;
import id.global.amqp.ExchangeTypeParser;
import id.global.amqp.MessageScopeParser;
import id.global.amqp.QueueAutoDeleteParser;
import id.global.amqp.QueueDurableParser;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
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

                    final var eventName = getMessageClassKebabCase(messageAnnotation);
                    final var name = messageAnnotation.value("name").asString();

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
                            eventName);
                });
    }

    private Predicate<AnnotationInstance> isNotSyntheticPredicate() {
        return annotationInstance -> !annotationInstance.target().asMethod().isSynthetic();
    }

    private AnnotationInstanceValidator getAnnotationValidator() {
        return new AnnotationInstanceValidator(index);
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
