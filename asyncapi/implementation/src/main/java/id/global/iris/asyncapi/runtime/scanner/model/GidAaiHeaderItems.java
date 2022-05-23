package id.global.iris.asyncapi.runtime.scanner.model;

import java.util.Map;

import io.apicurio.datamodels.asyncapi.models.AaiHeaderItem;

public class GidAaiHeaderItems extends AaiHeaderItem {
    private final String type;
    private final Map<String, GidAaiHeaderItem> properties;

    public GidAaiHeaderItems(String type,
            Map<String, GidAaiHeaderItem> properties) {
        this.type = type;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public Map<String, GidAaiHeaderItem> getProperties() {
        return properties;
    }
}
