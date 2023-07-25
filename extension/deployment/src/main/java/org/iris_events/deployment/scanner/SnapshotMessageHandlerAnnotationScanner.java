package org.iris_events.deployment.scanner;

import java.util.List;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.asyncapi.parsers.ConsumerPrefetchCountParser;
import org.iris_events.asyncapi.parsers.DeadLetterQueueParser;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.ExchangeTtlParser;
import org.iris_events.asyncapi.parsers.MessageScopeParser;
import org.iris_events.asyncapi.parsers.RolesAllowedParser;
import org.iris_events.common.HandlerDefaultParameter;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.constants.AnnotationInstanceParams;
import org.iris_events.deployment.validation.AnnotationInstanceValidator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class SnapshotMessageHandlerAnnotationScanner extends HandlerAnnotationScanner {

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
                methodInfo.parameterType(0),
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
