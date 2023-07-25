package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.MessageHandler;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

@SuppressWarnings("unused")
public class ConsumerPrefetchCountParser {

    private static final String MESSAGE_HANDLER_PREFETCH_COUNT_PARAM = "prefetchCount";

    public static int getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.prefetchCount();
    }

    public static int getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_PREFETCH_COUNT_PARAM).asInt();
    }
}
