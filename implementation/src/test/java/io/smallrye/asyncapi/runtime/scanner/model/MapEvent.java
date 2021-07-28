package io.smallrye.asyncapi.runtime.scanner.model;

import java.util.Map;

public class MapEvent {

    private Map<String, Object> mapProperty;

    public MapEvent() {
    }

    public MapEvent(Map<String, Object> mapProperty) {
        this.mapProperty = mapProperty;
    }

    public Map<String, Object> getMapProperty() {
        return mapProperty;
    }
}
