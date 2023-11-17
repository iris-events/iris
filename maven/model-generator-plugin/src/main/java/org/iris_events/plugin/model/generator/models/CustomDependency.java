package org.iris_events.plugin.model.generator.models;

import java.util.List;

public class CustomDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public CustomDependency(final List<String> dependencyParams) {
        this.groupId = dependencyParams.get(0);
        this.artifactId = dependencyParams.get(1);
        this.version = dependencyParams.get(2);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }
}
