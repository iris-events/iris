package id.global.iris.messaging.deployment.scanner;

import static id.global.iris.common.annotations.ExchangeType.FANOUT;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.messaging.deployment.builditem.MessageHandlerInfoBuildItem;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;
import id.global.iris.messaging.deployment.validation.AnnotationInstanceValidator;
import id.global.iris.parsers.BindingKeysParser;
import id.global.iris.parsers.ConsumerPerInstanceParser;
import id.global.iris.parsers.ConsumerPrefetchCountParser;
import id.global.iris.parsers.DeadLetterQueueParser;
import id.global.iris.parsers.ExchangeParser;
import id.global.iris.parsers.ExchangeTtlParser;
import id.global.iris.parsers.MessageScopeParser;
import id.global.iris.parsers.QueueAutoDeleteParser;
import id.global.iris.parsers.QueueDurableParser;
import id.global.iris.parsers.RolesAllowedParser;

public class MessageHandlerAnnotationScanner extends AnnotationScanner {

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
