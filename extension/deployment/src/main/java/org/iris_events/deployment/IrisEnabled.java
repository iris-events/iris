package org.iris_events.deployment;

import java.util.function.BooleanSupplier;

import org.iris_events.runtime.configuration.IrisBuildTimeConfig;

public class IrisEnabled implements BooleanSupplier {

    private final IrisBuildTimeConfig config;

    public IrisEnabled(IrisBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled;
    }
}
