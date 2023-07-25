package org.iris_events.deployment.scanner;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.stream.Collectors;

import org.iris_events.annotations.IrisGenerated;
import org.iris_events.deployment.builditem.MessageInfoBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class IrisGeneratedAnnotationScanner {
    // TODO this has alot of similarities with MessageAnnotationScanner
    private static final DotName DOT_NAME_IRIS_GENERATED = DotName.createSimple(IrisGenerated.class.getCanonicalName());

    public List<MessageInfoBuildItem> scanIrisGeneratedAnnotations(IndexView indexView) {
        return indexView.getAnnotations(DOT_NAME_IRIS_GENERATED)
                .stream()
                .filter(not(annotationInstance -> annotationInstance.target().asClass().isSynthetic()))
                .map(this::build)
                .collect(Collectors.toList());
    }

    protected MessageInfoBuildItem build(AnnotationInstance annotationInstance) {
        final var classType = annotationInstance.target().asClass();
        return new MessageInfoBuildItem(classType);
    }
}
