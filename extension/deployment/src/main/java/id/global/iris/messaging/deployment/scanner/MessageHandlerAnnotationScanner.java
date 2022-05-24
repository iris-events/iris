package id.global.iris.messaging.deployment.scanner;

import static id.global.common.iris.annotations.ExchangeType.FANOUT;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.common.iris.annotations.ExchangeType;
import id.global.common.iris.annotations.MessageHandler;
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
    }
}
