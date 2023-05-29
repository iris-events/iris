package org.iris_events.asyncapi.parsers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import org.iris_events.annotations.MessageHandler;

@SuppressWarnings("unused")
public class ConsumerPerInstanceParser {

    private static final String MESSAGE_HANDLER_CONSUMER_PER_INSTANCE_PARAM = "perInstance";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.perInstance();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_CONSUMER_PER_INSTANCE_PARAM).asBoolean();
    }
}
