package org.iris_events.deployment.scanner;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.stream.Collectors;

import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.asyncapi.IrisAnnotationRuntimeException;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.RpcResponseClassParser;
import org.iris_events.deployment.builditem.MessageInfoBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class IrisGeneratedAnnotationScanner {
    private static final DotName DOT_NAME_IRIS_GENERATED = DotName.createSimple(IrisGenerated.class.getCanonicalName());
    private static final DotName DOT_NAME_MESSAGE = DotName.createSimple(Message.class.getCanonicalName());

    public List<MessageInfoBuildItem> scanIrisGeneratedAnnotations(IndexView indexView) {
        return indexView.getAnnotations(DOT_NAME_IRIS_GENERATED)
                .stream()
                .filter(not(annotationInstance -> annotationInstance.target().asClass().isSynthetic()))
                .map(item -> build(item, indexView))
                .collect(Collectors.toList());
    }

    protected MessageInfoBuildItem build(AnnotationInstance annotationInstance, final IndexView indexView) {
        final var irisGeneratedClassType = annotationInstance.target().asClass();
        final var messageAnnotation = indexView.getAnnotations(DOT_NAME_MESSAGE).stream()
                .filter(annotation -> annotation.target().asClass().equals(irisGeneratedClassType))
                .findFirst().orElseThrow(() -> new IrisAnnotationRuntimeException(
                        "Class annotated with " + annotationInstance + " is missing " + Message.class + " annotation"));

        final var name = ExchangeParser.getFromAnnotationInstance(messageAnnotation);
        final var rpcResponseType = RpcResponseClassParser.getFromAnnotationInstance(messageAnnotation, indexView);

        return new MessageInfoBuildItem(irisGeneratedClassType, name, rpcResponseType);
    }
}
