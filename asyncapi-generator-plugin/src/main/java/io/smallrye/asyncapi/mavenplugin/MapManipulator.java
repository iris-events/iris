package io.smallrye.asyncapi.mavenplugin;

import java.util.List;
import java.util.Map;

public class MapManipulator {
    public static void addToPropertyMap(Map<String, String> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }

    public static void addToPropertyMap(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public static void addToPropertyMap(Map<String, String> map, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            map.put(key, String.join(",", values));
        }
    }
}
