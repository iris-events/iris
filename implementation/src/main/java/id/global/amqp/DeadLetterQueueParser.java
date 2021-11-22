package id.global.amqp;

import java.lang.reflect.Method;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Message;

public class DeadLetterQueueParser {

    private static final String MESSAGE_DEAD_LETTER_PARAM = "deadLetter";

    public static String getFromAnnotationClass(Message messageAnnotation) throws NoSuchMethodException {
        final var deadLetter = messageAnnotation.deadLetter();
        if (!Objects.isNull(deadLetter)) {
            return deadLetter;
        }
        Method deadLetterMethod = messageAnnotation.annotationType().getMethod(MESSAGE_DEAD_LETTER_PARAM);
        return (String) deadLetterMethod.getDefaultValue();
    }

    public static String getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return messageAnnotation.valueWithDefault(index, MESSAGE_DEAD_LETTER_PARAM).asString();
    }
}
