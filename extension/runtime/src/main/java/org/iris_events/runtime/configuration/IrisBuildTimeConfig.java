package org.iris_events.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.iris")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface IrisBuildTimeConfig {
    /**
     * disable initialization of consumers
     */

    @WithDefault("true")
    boolean enabled();

    /**
     * Enable or disable extension liveness health check
     */
    @WithDefault("true")
    boolean livenessCheckEnabled();

    /**
     * Enable or disable extension readiness health check
     */
    @WithDefault("true")
    boolean readinessCheckEnabled();

}
