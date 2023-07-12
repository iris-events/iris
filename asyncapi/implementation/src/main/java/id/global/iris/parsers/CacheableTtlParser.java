package id.global.iris.parsers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.iris.common.annotations.CachedMessage;

public class CacheableTtlParser {

    private static final String MESSAGE_TTL_PARAM = "ttl";

    public static int getFromAnnotationClass(CachedMessage cachedMessageAnnotation) {
        return cachedMessageAnnotation.ttl();
    }

    public static int getFromAnnotationInstance(AnnotationInstance cachedMessageAnnotation, IndexView index) {
        return cachedMessageAnnotation.valueWithDefault(index, MESSAGE_TTL_PARAM).asInt();
    }
}
