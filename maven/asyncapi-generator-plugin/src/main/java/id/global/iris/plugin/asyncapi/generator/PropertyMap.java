package id.global.iris.plugin.asyncapi.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyMap {
    private final Map<String, String> propertyMap;

    public PropertyMap() {
        this.propertyMap = new HashMap<>();
    }

    public void putAll(Map<String, String> p) {
        this.propertyMap.putAll(p);
    }

    public void put(String key, String value) {
        this.propertyMap.put(key, value);
    }

    public void put(String key, Boolean value) {
        if (value != null) {
            this.propertyMap.put(key, value.toString());
        }
    }

    public void put(String key, List<String> values) {
        this.propertyMap.put(key, String.join(",", values));
    }

    public Map<String, String> getMap() {
        return propertyMap;
    }
}
