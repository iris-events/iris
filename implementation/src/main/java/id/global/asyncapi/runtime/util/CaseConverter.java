package id.global.asyncapi.runtime.util;

public class CaseConverter {
    public static String camelToKebabCase(final String str) {
        return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}
