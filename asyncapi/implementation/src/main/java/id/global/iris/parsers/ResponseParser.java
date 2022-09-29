package id.global.iris.parsers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

public class ResponseParser {
    private static final String MESSAGE_RESPONSE_PARAM = "response";

    public static Type getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return messageAnnotation.valueWithDefault(index, MESSAGE_RESPONSE_PARAM).asClass();
    }
}
