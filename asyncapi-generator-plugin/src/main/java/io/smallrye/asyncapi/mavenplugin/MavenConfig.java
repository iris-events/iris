package io.smallrye.asyncapi.mavenplugin;

import static io.smallrye.asyncapi.api.util.ConfigUtil.asCsvSet;
import static io.smallrye.asyncapi.api.util.ConfigUtil.patternFromSet;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.api.AsyncApiConstants;
import io.smallrye.asyncapi.spec.AAIConfig;

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
        return asCsvSet(scanPackages);
    }

    @Override
    public Pattern scanPackagesPattern() {
        return patternFromSet(scanPackages());
    }

    @Override
    public Set<String> scanClasses() {
        String scanClasses = properties.getOrDefault(AAIConfig.SCAN_CLASSES, null);
        return asCsvSet(scanClasses);
    }

    @Override
    public Pattern scanClassesPattern() {
        return patternFromSet(scanClasses());
    }

    @Override
    public Set<String> scanExcludePackages() {
        String scanExcludePackages = properties.getOrDefault(AAIConfig.SCAN_EXCLUDE_PACKAGES, null);
        return asCsvSet(scanExcludePackages);
    }

    @Override
    public Pattern scanExcludePackagesPattern() {
        return patternFromSet(scanExcludePackages());
    }

    @Override
    public Set<String> scanExcludeClasses() {
        String scanExcludeClasses = properties.getOrDefault(AAIConfig.SCAN_EXCLUDE_CLASSES, null);
        return asCsvSet(scanExcludeClasses);
    }

    @Override
    public Pattern scanExcludeClassesPattern() {
        return patternFromSet(scanExcludeClasses());
    }

    @Override
    public Set<String> servers() {
        return asCsvSet(properties.getOrDefault(AAIConfig.SERVERS, null));
    }

    @Override
    public boolean scanDependenciesDisable() {
        return Boolean.parseBoolean(properties.getOrDefault(AsyncApiConstants.SCAN_DEPENDENCIES_DISABLE, "false"));
    }

    @Override
    public Set<String> scanDependenciesJars() {
        return asCsvSet(properties.getOrDefault(AsyncApiConstants.SCAN_DEPENDENCIES_JARS, null));
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
        return asCsvSet(properties.getOrDefault(AsyncApiConstants.EXCLUDE_FROM_SCHEMAS, null));
    }

    @Override public String projectVersion() {
        return properties.getOrDefault(AsyncApiConstants.PROJECT_VERSION, null);
    }
}
