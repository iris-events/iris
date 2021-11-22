package id.global.amqp;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.MessageHandler;

public class QueueDurableParser {

    private static final String MESSAGE_HANDLER_DURABLE_PARAM = "durable";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.durable();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_DURABLE_PARAM).asBoolean();
    }
}
