package org.iris_events.asyncapi.parsers;

import org.jboss.jandex.AnnotationInstance;

import org.iris_events.annotations.Message;

public class ExchangeParser {

    private static final String MESSAGE_NAME_PARAM = "name";

    public static String getFromAnnotationClass(Message messageAnnotation) {
        return messageAnnotation.name();
    }

    public static String getFromAnnotationInstance(AnnotationInstance messageAnnotation) {
        return messageAnnotation.value(MESSAGE_NAME_PARAM).asString();
    }

}
