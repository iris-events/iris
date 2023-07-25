package org.iris_events.asyncapi.parsers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.IndexView;

public class RolesAllowedParser {
    private static final String ROLES_ALLOWED_PARAM = "rolesAllowed";

    public static Set<String> getFromHandlerAnnotationClass(MessageHandler messageHandler) {
        final var rolesAllowed = messageHandler.rolesAllowed().value();
        return new HashSet<>(Arrays.asList(rolesAllowed));
    }

    public static Set<String> getFromMessageAnnotationClass(Message message) {
        final var rolesAllowed = message.rolesAllowed().value();
        return new HashSet<>(Arrays.asList(rolesAllowed));
    }

    public static Set<String> getFromAnnotationInstance(final AnnotationInstance annotation, IndexView index) {
        return annotation.valueWithDefault(index, ROLES_ALLOWED_PARAM).asNested().valueWithDefault(index).asArrayList()
                .stream()
                .map(AnnotationValue::asString)
                .collect(Collectors.toSet());
    }

    public static String getFromAnnotationInstanceAsCsv(final AnnotationInstance annotation, IndexView index) {
        return getFromAnnotationInstance(annotation, index)
                .stream()
                .collect(Collectors.joining(","));
    }
}
