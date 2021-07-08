package id.global.event.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
@ConfigGroup
public class ProducerConfiguration {
    /**
     * rabbit password
     */
    @ConfigItem(defaultValue = "false")
    boolean declare;



}
