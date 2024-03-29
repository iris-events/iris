package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.MessageHandler;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

public class QueueAutoDeleteParser {

    private static final String MESSAGE_HANDLER_AUTODELETE_PARAM = "autoDelete";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.autoDelete();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_AUTODELETE_PARAM).asBoolean();
    }
}
