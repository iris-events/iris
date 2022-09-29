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

    /**
     * Enable or disable extension readiness check
     */
    @ConfigItem(defaultValue = "true")
    public boolean readinessCheckEnabled;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public void setReadinessCheckEnabled(final boolean readinessCheckEnabled) {
        this.readinessCheckEnabled = readinessCheckEnabled;
    }

    @Override
    public String toString() {
        return "AmqpBuildConfiguration{" +
                "enabled=" + enabled +
                ", readinessEnabled=" + readinessCheckEnabled +
                ", healthEnabled=" + healthCheckEnabled +
                '}';
    }
}
