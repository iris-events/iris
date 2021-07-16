package io.smallrye.asyncapi.api.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigUtil {
    private ConfigUtil() {
    }

    public static Set<String> asCsvSet(String items) {
        Set<String> rval = new HashSet<>();
        if (items != null) {
            String[] split = items.split(",");
            for (String item : split) {
                rval.add(item.trim());
            }
        }
        return rval;
    }

    public static Pattern patternFromSet(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return Pattern.compile("");
        }
        return Pattern.compile(
                "(" + set.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")");
    }
}
