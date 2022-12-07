package id.global.iris.messaging.deployment.scanner;

import id.global.iris.common.annotations.Message;
import id.global.iris.messaging.deployment.MessageInfoBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class MessageAnnotationScanner {
    private static final DotName DOT_NAME_MESSAGE = DotName.createSimple(Message.class.getCanonicalName());

    public List<MessageInfoBuildItem> scanHandlerAnnotations(IndexView indexView) {
        return indexView.getAnnotations(DOT_NAME_MESSAGE)
                .stream()
                .filter(not(annotationInstance -> annotationInstance.target().asClass().isSynthetic()))
                .map(this::build)
                .collect(Collectors.toList());
    }

    protected MessageInfoBuildItem build(AnnotationInstance annotationInstance) {
        final var classInfo = annotationInstance.target().asClass();
        return new MessageInfoBuildItem(classInfo.name());
    }
}
