package org.iris_events.runtime;

import java.util.Optional;

import org.iris_events.exception.IrisSendException;

public class AnnotationValueExtractor {
    public static org.iris_events.annotations.Message getMessageAnnotation(final Object message) {
        if (message == null) {
            throw new IrisSendException("Can not extract message annotation from null");
        }

        return Optional
                .ofNullable(message.getClass().getAnnotation(org.iris_events.annotations.Message.class))
                .orElseThrow(() -> new IrisSendException("Message object not annotated with Message annotation"));
    }
}
