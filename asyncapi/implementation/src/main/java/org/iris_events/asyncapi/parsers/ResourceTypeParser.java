package org.iris_events.asyncapi.parsers;

import org.iris_events.annotations.SnapshotMessageHandler;
import org.jboss.jandex.AnnotationInstance;

public class ResourceTypeParser {

    private static final String RESOURCE_TYPE_PARAM = "resourceType";

    public static String getFromAnnotationClass(SnapshotMessageHandler messageHandler) {
        return messageHandler.resourceType();
    }

    public static String getFromAnnotationInstance(final AnnotationInstance snapshotMessageHandlerAnnotation) {
        return snapshotMessageHandlerAnnotation.value(RESOURCE_TYPE_PARAM).asString();
    }

}
