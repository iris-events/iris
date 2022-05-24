package id.global.iris.amqp.parsers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.iris.common.annotations.MessageHandler;

public class QueueAutoDeleteParser {

    private static final String MESSAGE_HANDLER_AUTODELETE_PARAM = "autoDelete";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.autoDelete();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_AUTODELETE_PARAM).asBoolean();
    }
}
