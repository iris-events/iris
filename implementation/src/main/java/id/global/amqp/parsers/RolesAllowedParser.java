package id.global.amqp.parsers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.MessageHandler;

public class RolesAllowedParser {
    private static final String MESSAGE_HANDLER_ROLES_ALLOWED_PARAM = "rolesAllowed";

    public static String[] getFromAnnotationClass(MessageHandler messageHandler) {
        return messageHandler.rolesAllowed();
    }

    public static String[] getFromAnnotationInstance(final AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, MESSAGE_HANDLER_ROLES_ALLOWED_PARAM).asStringArray();
    }

    public static String getFromAnnotationInstanceAsCsv(final AnnotationInstance annotation, IndexView index) {
        return String.join(",", getFromAnnotationInstance(annotation, index));
    }
}
