package id.global.iris.asyncapi.runtime.scanner.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public class GidAaiHeaderItem {
    private final String description;
    private final String type;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final Object value;

    public GidAaiHeaderItem(String description, String type, Object value) {
        this.description = description;
        this.type = type;
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
