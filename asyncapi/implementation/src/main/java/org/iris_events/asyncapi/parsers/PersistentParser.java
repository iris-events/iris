package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.Message;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

public class PersistentParser {

    private static final String MESSAGE_PERSISTENT_PARAM = "persistent";

    public static boolean getFromAnnotationClass(Message messageAnnotation) {
        return messageAnnotation.persistent();
    }

    public static boolean getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return messageAnnotation.valueWithDefault(index, MESSAGE_PERSISTENT_PARAM).asBoolean();
    }
}
