package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.Message;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

public class ExchangeTtlParser {

    private static final String MESSAGE_TTL_PARAM = "ttl";

    public static Integer getFromAnnotationClass(Message messageAnnotation) {
        return messageAnnotation.ttl();
    }

    public static Integer getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return messageAnnotation.valueWithDefault(index, MESSAGE_TTL_PARAM).asInt();
    }
}
