package org.iris_events.deployment;

import org.iris_events.auth.IrisJwtValidator;
import org.iris_events.health.IrisLivenessCheck;
import org.iris_events.health.IrisReadinessCheck;
import org.iris_events.runtime.configuration.IrisBuildTimeConfig;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class CapabilitiesProcessor {

    @BuildStep
    AdditionalBeanBuildItem addHealthChecks(Capabilities capabilities,
            BuildProducer<HealthBuildItem> healthChecks,
            IrisBuildTimeConfig config) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            healthChecks
                    .produce(new HealthBuildItem("org.iris_events.health.IrisLivenessCheck", config.livenessCheckEnabled()));
            healthChecks
                    .produce(new HealthBuildItem("org.iris_events.health.IrisReadinessCheck", config.readinessCheckEnabled()));
            return new AdditionalBeanBuildItem.Builder()
                    .addBeanClasses(
                            IrisReadinessCheck.class,
                            IrisLivenessCheck.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .build();
        }
        return null;

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
