package id.global.event.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ConsumerConfiguration {
    /**
     * rabbit password
     */
    @ConfigItem(defaultValue = "false")
    boolean declare;

    public boolean isDeclare() {
        return declare;
    }

    public void setDeclare(boolean declare) {
        this.declare = declare;
    }
}
