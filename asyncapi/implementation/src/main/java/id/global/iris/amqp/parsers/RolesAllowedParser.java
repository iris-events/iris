package id.global.iris.amqp.parsers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.auth.jwt.Role;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;

public class RolesAllowedParser {
    private static final String ROLES_ALLOWED_PARAM = "rolesAllowed";

    public static Set<Role> getFromHandlerAnnotationClass(MessageHandler messageHandler) {
        final var rolesAllowed = messageHandler.rolesAllowed();
        return new HashSet<>(Arrays.asList(rolesAllowed));
    }

    public static Set<Role> getFromMessageAnnotationClass(Message message) {
        final var rolesAllowed = message.rolesAllowed();
        return new HashSet<>(Arrays.asList(rolesAllowed));
    }

    public static Set<Role> getFromAnnotationInstance(final AnnotationInstance annotation, IndexView index) {
        return Arrays.stream(annotation.valueWithDefault(index, ROLES_ALLOWED_PARAM).asEnumArray())
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    public static String getFromAnnotationInstanceAsCsv(final AnnotationInstance annotation, IndexView index) {
        return getFromAnnotationInstance(annotation, index)
                .stream()
                .map(Role::value)
                .collect(Collectors.joining(","));
    }
}
