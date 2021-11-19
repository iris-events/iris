package id.global.amqp;

import org.jboss.jandex.AnnotationInstance;

import id.global.asyncapi.runtime.scanner.FilteredIndexView;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;

public class ExchangeDurableParser {

    private static final String MESSAGE_HANDLER_DURABLE_PARAM = "durable";

    public static boolean getFromAnnotationClass(MessageHandler messageHandlerAnnotation) {
        return messageHandlerAnnotation.durable();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance annotation,
            FilteredIndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_DURABLE_PARAM).asBoolean();
    }
}
