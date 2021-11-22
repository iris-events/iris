package id.global.amqp;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.MessageHandler;

public class ConsumerPrefetchCountParser {

    private static final String MESSAGE_HANDLER_PREFETCH_COUNT_PARAM = "prefetchCount";

    public static long getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.prefetchCount();
    }

    public static long getFromAnnotationInstance(AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_PREFETCH_COUNT_PARAM).asLong();
    }
}
