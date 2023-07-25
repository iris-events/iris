package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.MessageHandler;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

public class QueueDurableParser {

    private static final String MESSAGE_HANDLER_DURABLE_PARAM = "durable";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.durable();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_DURABLE_PARAM).asBoolean();
    }
}
