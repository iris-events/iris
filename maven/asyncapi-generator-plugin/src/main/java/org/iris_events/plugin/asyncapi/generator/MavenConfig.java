package org.iris_events.plugin.asyncapi.generator;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.iris_events.asyncapi.api.AsyncApiConfig;
import org.iris_events.asyncapi.api.AsyncApiConstants;
import org.iris_events.asyncapi.spec.AAIConfig;
import org.iris_events.asyncapi.api.util.ConfigUtil;

/**
 * Implementation of the {@link AsyncApiConfig} interface that gets config information from maven
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class MavenConfig implements AsyncApiConfig {

    private final Map<String, String> properties;

    public MavenConfig(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String modelReader() {
        return properties.getOrDefault(AAIConfig.MODEL_READER, null);
    }

    @Override
    public String filter() {
        return properties.getOrDefault(AAIConfig.FILTER, null);
    }

    @Override
    public boolean scanDisable() {
        return Boolean.parseBoolean(properties.getOrDefault(AAIConfig.FILTER, "false"));
    }

    @Override
    public Set<String> scanPackages() {
        String scanPackages = properties.getOrDefault(AAIConfig.SCAN_PACKAGES, null);
        return ConfigUtil.asCsvSet(scanPackages);
    }

    @Override
    public Pattern scanPackagesPattern() {
        return ConfigUtil.patternFromSet(scanPackages());
    }

    @Override
    public Set<String> scanClasses() {
        String scanClasses = properties.getOrDefault(AAIConfig.SCAN_CLASSES, null);
        return ConfigUtil.asCsvSet(scanClasses);
    }

    @Override
    public Pattern scanClassesPattern() {
        return ConfigUtil.patternFromSet(scanClasses());
    }

    @Override
    public Set<String> scanExcludePackages() {
        String scanExcludePackages = properties.getOrDefault(AAIConfig.SCAN_EXCLUDE_PACKAGES, null);
        return ConfigUtil.asCsvSet(scanExcludePackages);
    }

    @Override
    public Pattern scanExcludePackagesPattern() {
        return ConfigUtil.patternFromSet(scanExcludePackages());
    }

    @Override
    public Set<String> scanExcludeClasses() {
        String scanExcludeClasses = properties.getOrDefault(AAIConfig.SCAN_EXCLUDE_CLASSES, null);
        return ConfigUtil.asCsvSet(scanExcludeClasses);
    }

    @Override
    public Pattern scanExcludeClassesPattern() {
        return ConfigUtil.patternFromSet(scanExcludeClasses());
    }

    @Override
    public Set<String> servers() {
        return ConfigUtil.asCsvSet(properties.getOrDefault(AAIConfig.SERVERS, null));
    }

    @Override
    public boolean scanDependenciesDisable() {
        return Boolean.parseBoolean(properties.getOrDefault(AsyncApiConstants.SCAN_DEPENDENCIES_DISABLE, "false"));
    }

    @Override
    public Set<String> scanDependenciesJars() {
        return ConfigUtil.asCsvSet(properties.getOrDefault(AsyncApiConstants.SCAN_DEPENDENCIES_JARS, null));
    }

    @Override
    public boolean schemaReferencesEnable() {
        return Boolean.parseBoolean(properties.getOrDefault(AsyncApiConstants.SCHEMA_REFERENCES_ENABLE, "true"));
    }

    @Override
    public String customSchemaRegistryClass() {
        return properties.getOrDefault(AsyncApiConstants.CUSTOM_SCHEMA_REGISTRY_CLASS, null);
    }

    @Override
    public Set<String> excludeFromSchemas() {
        return ConfigUtil.asCsvSet(properties.getOrDefault(AsyncApiConstants.EXCLUDE_FROM_SCHEMAS, null));
    }

    @Override
    public String projectVersion() {
        return properties.getOrDefault(AsyncApiConstants.PROJECT_VERSION, null);
    }
}
