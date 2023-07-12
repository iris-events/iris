package id.global.iris.messaging.deployment.scanner;

import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.*;

import id.global.iris.common.annotations.CachedMessage;
import id.global.iris.common.annotations.Message;

public class ScannerUtils {

    private static final DotName DOT_NAME_MESSAGE = DotName.createSimple(Message.class.getCanonicalName());
    private static final DotName DOT_NAME_CACHED_MESSAGE = DotName.createSimple(CachedMessage.class.getCanonicalName());

    public static AnnotationInstance getMessageAnnotation(final MethodInfo methodInfo, final IndexView index) {
        final var consumedClassInfo = getConsumedEventClassInfo(methodInfo, index);
        final var annotationInstance = consumedClassInfo.declaredAnnotation(DOT_NAME_MESSAGE);

        if (Objects.isNull(annotationInstance)) {
            throw new IllegalArgumentException(String.format("Consumed Event requires %s annotation for method %s in class %s.",
                    DOT_NAME_MESSAGE, methodInfo.name(), methodInfo.declaringClass()));
        }

        return annotationInstance;
    }

    private static ClassInfo getConsumedEventClassInfo(final MethodInfo methodInfo, final IndexView index) {
        final var parameters = methodInfo.parameterTypes();
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

    /**
     * Returns cacheable annotation if message is annotated as {@link CachedMessage}.
     *
     * @param classInfo Message class info
     * @return Cacheable annotation if present
     */
    public static Optional<AnnotationInstance> getCacheableAnnotation(final ClassInfo classInfo) {
        return Optional.ofNullable(classInfo.declaredAnnotation(DOT_NAME_CACHED_MESSAGE));
    }
}
