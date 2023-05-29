package org.iris_events.asyncapi.runtime.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.iris_events.asyncapi.api.AsyncApiConstants;

/**
 * Some utility methods for working with Jandex objects.
 */
public class JandexUtil {

    /**
     * Constructor.
     */
    private JandexUtil() {
    }

    /**
     * Convenience method to retrieve the named parameter from an annotation.
     * The value will be unwrapped from its containing {@link AnnotationValue}.
     *
     * @param <T> the type of the parameter being retrieved
     * @param annotation the annotation from which to fetch the parameter
     * @param name the name of the parameter
     * @return an unwrapped annotation parameter value
     */
    @SuppressWarnings({ "unchecked", "squid:S3776" })
    public static <T> T value(AnnotationInstance annotation, String name) {
        final AnnotationValue value = annotation.value(name);

        if (value == null) {
            return null;
        }

        final boolean isArray = (AnnotationValue.Kind.ARRAY == value.kind());

        switch (isArray ? value.componentKind() : value.kind()) {
            case BOOLEAN:
                return (T) (isArray ? value.asBooleanArray() : value.asBoolean());
            case BYTE:
                return (T) (isArray ? value.asByteArray() : value.asByte());
            case CHARACTER:
                return (T) (isArray ? value.asCharArray() : value.asChar());
            case CLASS:
                return (T) (isArray ? value.asClassArray() : value.asClass());
            case DOUBLE:
                return (T) (isArray ? value.asDoubleArray() : value.asDouble());
            case ENUM:
                return (T) (isArray ? value.asEnumArray() : value.asEnum());
            case FLOAT:
                return (T) (isArray ? value.asFloatArray() : value.asFloat());
            case INTEGER:
                return (T) (isArray ? value.asIntArray() : value.asInt());
            case LONG:
                return (T) (isArray ? value.asLongArray() : value.asLong());
            case NESTED:
                return (T) (isArray ? value.asNestedArray() : value.asNested());
            case SHORT:
                return (T) (isArray ? value.asShortArray() : value.asShort());
            case STRING:
                return (T) (isArray ? value.asStringArray() : value.asString());
            case UNKNOWN:
            default:
                return null;
        }
    }

    /**
     * Convenience method to retrieve the named parameter from an annotation.
     * The value will be unwrapped from its containing {@link AnnotationValue}.
     *
     * @param <T> the type of the parameter being retrieved
     * @param annotation the annotation from which to fetch the parameter
     * @param name the name of the parameter
     * @param defaultValue a default value to return if the parameter is not defined
     * @return an unwrapped annotation parameter value
     */
    public static <T> T value(AnnotationInstance annotation, String name, T defaultValue) {
        T value = JandexUtil.value(annotation, name);
        return value != null ? value : defaultValue;
    }

    /**
     * Reads a String property value from the given annotation instance. If no value is found
     * this will return null.
     *
     * @param annotation AnnotationInstance
     * @param propertyName String
     * @return String value
     */
    public static String stringValue(AnnotationInstance annotation, String propertyName) {
        AnnotationValue value = annotation.value(propertyName);
        if (value == null) {
            return null;
        } else {
            return value.asString();
        }
    }

    public static Optional<String> optionalStringValue(AnnotationInstance annotation, String propertyName) {
        String value = stringValue(annotation, propertyName);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * Reads a String array property value from the given annotation instance. If no value is found
     * this will return null.
     *
     * @param annotation AnnotationInstance
     * @param propertyName String
     * @return List of Strings
     */
    public static Optional<List<String>> stringListValue(AnnotationInstance annotation, String propertyName) {
        AnnotationValue value = annotation.value(propertyName);
        if (value != null) {
            return Optional.of(new ArrayList<>(Arrays.asList(value.asStringArray())));
        }
        return Optional.empty();
    }

    /**
     * Reads a String property value from the given annotation instance. If no value is found
     * this will return null.
     *
     * @param annotation AnnotationInstance
     * @param propertyName String
     * @param clazz Class type of the Enum
     * @param <T> Type parameter
     * @return Value of property
     */
    public static <T extends Enum<?>> T enumValue(AnnotationInstance annotation, String propertyName, Class<T> clazz) {
        AnnotationValue value = annotation.value(propertyName);
        if (value == null) {
            return null;
        }
        return enumValue(value.asString(), clazz);
    }

    /**
     * Converts a string value to the given enum type. If the string does not match
     * one of the the enum's values name (case-insensitive) or toString value, null
     * will be returned.
     *
     * @param strVal String
     * @param clazz Class type of the Enum
     * @param <T> Type parameter
     * @return Value of property
     */
    public static <T extends Enum<?>> T enumValue(String strVal, Class<T> clazz) {
        T[] constants = clazz.getEnumConstants();
        for (T t : constants) {
            if (t.toString().equals(strVal)) {
                return t;
            }
        }
        for (T t : constants) {
            if (t.name().equalsIgnoreCase(strVal)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Returns true if the given annotation instance is a "ref". An annotation is a ref if it has
     * a non-null value for the "ref" property.
     *
     * @param annotation AnnotationInstance
     * @return Whether it's a "ref"
     */
    public static boolean isRef(AnnotationInstance annotation) {
        return annotation.value(AsyncApiConstants.REF) != null;
    }
}
