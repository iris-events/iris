package org.iris_events.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import org.iris_events.auth.IrisJwtValidator;
import org.iris_events.runtime.configuration.IrisBuildTimeConfig;

public class CapabilitiesProcessor {

    @BuildStep
    void addHealthChecks(Capabilities capabilities, BuildProducer<HealthBuildItem> healthChecks, IrisBuildTimeConfig config) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            healthChecks.produce(new HealthBuildItem("org.iris_events.health.IrisLivenessCheck", config.livenessCheckEnabled));
            healthChecks.produce(new HealthBuildItem("org.iris_events.health.IrisReadinessCheck", config.readinessCheckEnabled));
        }
    }

    @BuildStep
    AdditionalBeanBuildItem declareJwtBeans(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.JWT)) {
            return null;
        }
        return new AdditionalBeanBuildItem.Builder()
                .addBeanClass(IrisJwtValidator.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }


}
