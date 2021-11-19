package id.global.asyncapi.runtime.util;

import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import id.global.asyncapi.runtime.scanner.FilteredIndexView;

public class GidAnnotationParser {
    private static final String METHOD_NAME_ROLES_ALLOWED = "rolesAllowed";
    private static final String METHOD_NAME_DURABLE = "durable";
    private static final String METHOD_NAME_AUTODELETE = "autodelete";
    private static final String METHOD_NAME_DEAD_LETTER = "deadLetter";

    private static final String[] DEFAULT_ROLES_ALLOWED = new String[0];

    public static boolean getDurable(AnnotationInstance annotation,
            FilteredIndexView index) {
        return annotation.valueWithDefault(index, METHOD_NAME_DURABLE).asBoolean();
    }

    public static boolean getAutodelete(AnnotationInstance annotation, FilteredIndexView index) {
        return annotation.valueWithDefault(index, METHOD_NAME_AUTODELETE).asBoolean();
    }

    public static String getDeadLetterQueue(AnnotationInstance annotation, FilteredIndexView index) {
        return annotation.valueWithDefault(index, METHOD_NAME_DEAD_LETTER).asString();
    }

    public static String[] getRolesAllowed(AnnotationInstance annotation) {
        return Optional.ofNullable(annotation.value(METHOD_NAME_ROLES_ALLOWED))
                .map(AnnotationValue::asStringArray)
                .orElse(DEFAULT_ROLES_ALLOWED);
    }

    public static String camelToKebabCase(final String str) {
        return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}
