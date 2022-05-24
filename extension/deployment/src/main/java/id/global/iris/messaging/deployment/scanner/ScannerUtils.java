package id.global.iris.messaging.deployment.scanner;

import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import id.global.iris.common.annotations.Message;

public class ScannerUtils {

    private static final DotName DOT_NAME_MESSAGE = DotName.createSimple(Message.class.getCanonicalName());

    public static AnnotationInstance getMessageAnnotation(final MethodInfo methodInfo, final IndexView index) {
        final var consumedClassInfo = getConsumedEventClassInfo(methodInfo, index);
        final var annotationInstance = consumedClassInfo.classAnnotation(DOT_NAME_MESSAGE);

        if (Objects.isNull(annotationInstance)) {
            throw new IllegalArgumentException(String.format("Consumed Event requires %s annotation for method %s in class %s.",
                    DOT_NAME_MESSAGE, methodInfo.name(), methodInfo.declaringClass()));
        }

        return annotationInstance;
    }

    private static ClassInfo getConsumedEventClassInfo(final MethodInfo methodInfo, final IndexView index) {
        final var parameters = methodInfo.parameters();
        final var consumedEventTypes = parameters.stream()
                .map(Type::name)
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .toList();

        if (consumedEventTypes.isEmpty()) {
            throw new IllegalArgumentException(String.format("Consumed Event not found for method %s in class %s.",
                    methodInfo.name(), methodInfo.declaringClass()));
        }
        if (consumedEventTypes.size() > 1) {
            throw new IllegalArgumentException(String.format(
                    "Multiple consumed Events detected for method %s in class %s. Message handler can only handle one event type.",
                    methodInfo.name(), methodInfo.declaringClass()));
        }

        return consumedEventTypes.get(0);
    }
}
