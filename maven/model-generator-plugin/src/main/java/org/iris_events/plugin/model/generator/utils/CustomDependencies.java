package org.iris_events.plugin.model.generator.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.iris_events.plugin.model.generator.exception.AmqpGeneratorRuntimeException;
import org.iris_events.plugin.model.generator.models.CustomDependency;

public class CustomDependencies {
    private final static String DEP_PLACEHOLCER = "<dependency>\n" +
            "<groupId>$GROUP_ID$</groupId>\n" +
            "<artifactId>$ARTIFACT_ID$</artifactId>\n" +
            "<version>$VERSION$</version>\n" +
            "</dependency>";
    private final static String GROUP_ID = "$GROUP_ID$";
    private final static String ARTIFACT_ID = "$ARTIFACT_ID$";
    private final static String VERSOIN = "$VERSION$";
    private final List<CustomDependency> dependencies;

    public CustomDependencies(final String customDependencyArgument) {
        this.dependencies = parse(customDependencyArgument);
    }

    public String getDependenciesValue() {
        final var sBuilder = new StringBuilder("");
        dependencies.forEach(dep -> {
            final var dependencyBlock = DEP_PLACEHOLCER.replace(GROUP_ID, dep.getGroupId())
                    .replace(ARTIFACT_ID, dep.getArtifactId())
                    .replace(VERSOIN, dep.getVersion());
            sBuilder.append(dependencyBlock).append("\n");
        });
        return sBuilder.toString();
    }

    private List<CustomDependency> parse(final String customDependency) {
        final var trimmed = customDependency.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        final var depStrings = Arrays.asList(trimmed.split(","));
        return depStrings.stream().map(depString -> {
            final var split = Arrays.asList(depString.trim().split(":"));

            if (split.size() != 3) {
                throw new AmqpGeneratorRuntimeException("Custom dependency in wrong format. Use {groupId:artifactId:version}");
            }
            return new CustomDependency(split);
        }).collect(Collectors.toList());
    }
}
