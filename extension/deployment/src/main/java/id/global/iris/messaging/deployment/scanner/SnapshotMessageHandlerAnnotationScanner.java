package id.global.iris.messaging.deployment.scanner;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.iris.amqp.parsers.ConsumerPrefetchCountParser;
import id.global.iris.amqp.parsers.DeadLetterQueueParser;
import id.global.iris.amqp.parsers.ExchangeParser;
import id.global.iris.amqp.parsers.ExchangeTtlParser;
import id.global.iris.amqp.parsers.MessageScopeParser;
import id.global.iris.amqp.parsers.RolesAllowedParser;
import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.SnapshotMessageHandler;
import id.global.iris.common.constants.HandlerDefaultParameter;
import id.global.iris.messaging.deployment.MessageHandlerInfoBuildItem;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;
import id.global.iris.messaging.deployment.validation.AnnotationInstanceValidator;

public class SnapshotMessageHandlerAnnotationScanner extends AnnotationScanner {

    private static final DotName DOT_NAME_SNAPSHOT_MESSAGE_HANDLER = DotName
            .createSimple(SnapshotMessageHandler.class.getCanonicalName());

    private final AnnotationInstanceValidator annotationInstanceValidator;

    public SnapshotMessageHandlerAnnotationScanner(AnnotationInstanceValidator annotationInstanceValidator) {
        this.annotationInstanceValidator = annotationInstanceValidator;
    }

    @Override
    protected DotName getAnnotationName() {
        return DOT_NAME_SNAPSHOT_MESSAGE_HANDLER;
    }

    @Override
    protected MessageHandlerInfoBuildItem build(AnnotationInstance annotationInstance, IndexView index) {
        annotationInstanceValidator.validate(annotationInstance);

        final var methodInfo = annotationInstance.target().asMethod();
        final var messageAnnotation = ScannerUtils.getMessageAnnotation(methodInfo, index);
        final var exchangeType = ExchangeType.valueOf(
                messageAnnotation.valueWithDefault(index, AnnotationInstanceParams.EXCHANGE_TYPE_PARAM).asString());

        final var exchange = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
        final var scope = MessageScopeParser.getFromAnnotationInstance(messageAnnotation, index);
        final var ttl = ExchangeTtlParser.getFromAnnotationInstance(messageAnnotation, index);
        final var deadLetter = DeadLetterQueueParser.getFromAnnotationInstance(messageAnnotation, index);

        final var bindingKeys = List.of(annotationInstance.value(AnnotationInstanceParams.RESOURCE_TYPE_PARAM).asString());
        final var durable = HandlerDefaultParameter.SnapshotMessageHandler.DURABLE;
        final var autoDelete = HandlerDefaultParameter.SnapshotMessageHandler.AUTO_DELETE;
        final var perInstance = HandlerDefaultParameter.SnapshotMessageHandler.PER_INSTANCE;
        final var prefetchCount = ConsumerPrefetchCountParser.getFromAnnotationInstance(annotationInstance,
                index);
        final var handlerRolesAllowed = RolesAllowedParser.getFromAnnotationInstance(annotationInstance,
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
    }
}
