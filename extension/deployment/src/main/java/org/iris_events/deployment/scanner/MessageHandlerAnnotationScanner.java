package org.iris_events.deployment.scanner;

import static org.iris_events.annotations.ExchangeType.FANOUT;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.constants.AnnotationInstanceParams;
import org.iris_events.deployment.validation.AnnotationInstanceValidator;
import org.iris_events.asyncapi.parsers.BindingKeysParser;
import org.iris_events.asyncapi.parsers.ConsumerPerInstanceParser;
import org.iris_events.asyncapi.parsers.ConsumerPrefetchCountParser;
import org.iris_events.asyncapi.parsers.DeadLetterQueueParser;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.ExchangeTtlParser;
import org.iris_events.asyncapi.parsers.MessageScopeParser;
import org.iris_events.asyncapi.parsers.QueueAutoDeleteParser;
import org.iris_events.asyncapi.parsers.QueueDurableParser;
import org.iris_events.asyncapi.parsers.RolesAllowedParser;

public class MessageHandlerAnnotationScanner extends HandlerAnnotationScanner {

    private static final DotName DOT_NAME_MESSAGE_HANDLER = DotName.createSimple(MessageHandler.class.getCanonicalName());

    private final AnnotationInstanceValidator annotationValidator;

    public MessageHandlerAnnotationScanner(AnnotationInstanceValidator annotationValidator) {
        this.annotationValidator = annotationValidator;
    }

    @Override
    protected DotName getAnnotationName() {
        return DOT_NAME_MESSAGE_HANDLER;
    }

    @Override
    protected MessageHandlerInfoBuildItem build(AnnotationInstance annotationInstance, IndexView index) {
        annotationValidator.validate(annotationInstance);
        final var methodInfo = annotationInstance.target().asMethod();

        final var messageAnnotation = ScannerUtils.getMessageAnnotation(methodInfo, index);
        final var exchangeType = ExchangeType.valueOf(
                messageAnnotation.valueWithDefault(index, AnnotationInstanceParams.EXCHANGE_TYPE_PARAM).asString());

        final var exchange = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
        final var scope = MessageScopeParser.getFromAnnotationInstance(messageAnnotation, index);
        final var ttl = ExchangeTtlParser.getFromAnnotationInstance(messageAnnotation, index);
        final var deadLetter = DeadLetterQueueParser.getFromAnnotationInstance(messageAnnotation, index);

        final var bindingKeys = exchangeType != FANOUT
                ? BindingKeysParser.getFromAnnotationInstance(annotationInstance, messageAnnotation)
                : null;
        final var durable = QueueDurableParser.getFromAnnotationInstance(annotationInstance, index);
        final var autoDelete = QueueAutoDeleteParser.getFromAnnotationInstance(annotationInstance, index);
        final var perInstance = ConsumerPerInstanceParser.getFromAnnotationInstance(annotationInstance, index);
        final var prefetchCount = ConsumerPrefetchCountParser.getFromAnnotationInstance(annotationInstance, index);
        final var handlerRolesAllowed = RolesAllowedParser.getFromAnnotationInstance(annotationInstance, index);

        return new MessageHandlerInfoBuildItem(
                methodInfo.declaringClass(),
                methodInfo.parameterTypes().get(0),
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
    }
}
