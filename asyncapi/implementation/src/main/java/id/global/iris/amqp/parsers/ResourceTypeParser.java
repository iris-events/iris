package id.global.iris.amqp.parsers;

import org.jboss.jandex.AnnotationInstance;

import id.global.common.iris.annotations.SnapshotMessageHandler;

public class ResourceTypeParser {

    private static final String RESOURCE_TYPE_PARAM = "resourceType";

    public static String getFromAnnotationClass(SnapshotMessageHandler messageHandler) {
        return messageHandler.resourceType();
    }

    public static String getFromAnnotationInstance(final AnnotationInstance snapshotMessageHandlerAnnotation) {
        return snapshotMessageHandlerAnnotation.value(RESOURCE_TYPE_PARAM).asString();
    }

}
