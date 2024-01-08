package org.iris_events.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class IrisBuildTimeConfig {
    /**
     * disable initialization of consumers
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Enable or disable extension liveness health check
     */
    @ConfigItem(defaultValue = "true")
    public boolean livenessCheckEnabled;

    /**
     * Enable or disable extension readiness helath check
     */
    @ConfigItem(defaultValue = "true")
    public boolean readinessCheckEnabled;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLivenessCheckEnabled(boolean livenessCheckEnabled) {
        this.livenessCheckEnabled = livenessCheckEnabled;
    }

    public void setReadinessCheckEnabled(final boolean readinessCheckEnabled) {
        this.readinessCheckEnabled = readinessCheckEnabled;
    }

    @Override
    public String toString() {
        return "IrisBuildConfiguration{" +
                "enabled=" + enabled +
                ", readinessEnabled=" + readinessCheckEnabled +
                ", livenessEnabled=" + livenessCheckEnabled +
                '}';
    }
}
