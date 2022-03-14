package id.global.iris.messaging.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "amqp", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class AmqpBuildConfiguration {
    /**
     * disable initialization of consumers
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Enable or disable extension health check
     */
    @ConfigItem(defaultValue = "true")
    public boolean healthCheckEnabled;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    @Override
    public String toString() {
        return "AmqpBuildConfiguration{" +
                "enabled=" + enabled +
                ", healthEnabled=" + healthCheckEnabled +
                '}';
    }
}
