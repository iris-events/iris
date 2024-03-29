package org.iris_events.asyncapi.parsers;

import java.lang.reflect.Method;
import java.util.Objects;

import org.iris_events.annotations.Message;
import org.iris_events.asyncapi.IrisAnnotationRuntimeException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

public class DeadLetterQueueParser {

    private static final String MESSAGE_DEAD_LETTER_PARAM = "deadLetter";

    public static String getFromAnnotationClass(Message messageAnnotation) {
        final var deadLetter = messageAnnotation.deadLetter();
        if (Objects.nonNull(deadLetter)) {
            return deadLetter;
        }
        try {
            Method deadLetterMethod = messageAnnotation.annotationType().getMethod(MESSAGE_DEAD_LETTER_PARAM);
            return (String) deadLetterMethod.getDefaultValue();
        } catch (NoSuchMethodException e) {
            throw new IrisAnnotationRuntimeException(
                    String.format("Malformed %s annotation. Does not contain %s parameter default",
                            Message.class.getName(),
                            MESSAGE_DEAD_LETTER_PARAM));
        }
    }

    public static String getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return messageAnnotation.valueWithDefault(index, MESSAGE_DEAD_LETTER_PARAM).asString();
    }
}
