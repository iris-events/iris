package org.iris_events.deployment.scanner;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.stream.Collectors;

import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public abstract class HandlerAnnotationScanner {

    public List<MessageHandlerInfoBuildItem> scanHandlerAnnotations(IndexView indexView) {
        return indexView.getAnnotations(getAnnotationName())
                .stream()
                .filter(not(annotationInstance -> annotationInstance.target().asMethod().isSynthetic()))
                .map(annotationInstance -> build(annotationInstance, indexView))
                .collect(Collectors.toList());
    }

    protected abstract DotName getAnnotationName();

    protected abstract MessageHandlerInfoBuildItem build(AnnotationInstance annotationInstance, IndexView index);

}
