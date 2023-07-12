package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.CachedMessage;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;


public class CacheableTtlParser {

    private static final String MESSAGE_TTL_PARAM = "ttl";

    public static int getFromAnnotationClass(CachedMessage cachedMessageAnnotation) {
        return cachedMessageAnnotation.ttl();
    }

    public static int getFromAnnotationInstance(AnnotationInstance cachedMessageAnnotation, IndexView index) {
        return cachedMessageAnnotation.valueWithDefault(index, MESSAGE_TTL_PARAM).asInt();
    }
}
