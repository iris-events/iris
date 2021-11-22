package id.global.amqp;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.Message;

public class ExchangeTtlParser {

    private static final String MESSAGE_TTL_PARAM = "ttl";

    public static Integer getFromAnnotationClass(Message messageAnnotation) {
        final var ttl = messageAnnotation.ttl();
        if (ttl > -1) {
            return ttl;
        }
        return null;
    }

    public static Integer getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        int ttl = messageAnnotation.valueWithDefault(index, MESSAGE_TTL_PARAM).asInt();
        if (ttl > -1) {
            return ttl;
        }
        return null;
    }
}
