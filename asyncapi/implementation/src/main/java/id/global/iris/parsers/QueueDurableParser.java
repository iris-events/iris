package id.global.iris.parsers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.iris.common.annotations.MessageHandler;

public class QueueDurableParser {

    private static final String MESSAGE_HANDLER_DURABLE_PARAM = "durable";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.durable();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_DURABLE_PARAM).asBoolean();
    }
}
