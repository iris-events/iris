package org.iris_events.plugin.asyncapi.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.iris_events.asyncapi.api.AsyncApiConfig;
import org.iris_events.asyncapi.api.AsyncApiConstants;
import org.iris_events.asyncapi.runtime.client.ApicurioClient;
import org.iris_events.asyncapi.runtime.io.AsyncApiSerializer;
import org.iris_events.asyncapi.runtime.io.Format;
import org.iris_events.asyncapi.runtime.json.IrisObjectMapper;
import org.iris_events.asyncapi.runtime.scanner.IrisAnnotationScanner;
import org.iris_events.asyncapi.spec.AAIConfig;
import org.jboss.jandex.IndexView;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;
import io.apicurio.registry.rest.client.RegistryClientFactory;

@SuppressWarnings("unused")
@Mojo(name = "generate-schema", defaultPhase = LifecyclePhase.DEPLOY, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateSchemaMojo extends AbstractMojo {

    /**
     * Directory where to output the schemas.
     * If no path is specified, the schema will be printed to the log.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/", property = "outputDirectory")
    private File outputDirectory;

    /**
     * Filename of the schema
     * Default to openapi. So the files created will be openapi.yaml and openapi.json.
     */
    @Parameter(defaultValue = "asyncapi", property = "schemaFilename")
    private String schemaFilename;

    /**
     * When you include dependencies, we only look at compile and system scopes (by default)
     * You can change that here.
     * Valid options are: compile, provided, runtime, system, test, import
     */
    @Parameter(defaultValue = "compile,system", property = "includeDependenciesScopes")
    private List<String> includeDependenciesScopes;

    /**
     * When you include dependencies, we only look at jars (by default)
     * You can change that here.
     */
    @Parameter(defaultValue = "jar", property = "includeDependenciesTypes")
    private List<String> includeDependenciesTypes;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "false", property = "skip")
    private boolean skip;

    /**
     * Compiled classes of the project.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDir")
    private File classesDir;

    @Parameter(defaultValue = "${project.version}", property = "projectVersion")
    private String projectVersion;

    @Parameter(property = "configProperties")
    private File configProperties;

    // Properies as per AsyncAPI Config.

    @Parameter(property = "modelReader")
    private String modelReader;

    @Parameter(property = "filter")
    private String filter;

    @Parameter(property = "scanDisabled")
    private Boolean scanDisabled;

    @Parameter(property = "scanPackages")
    private String scanPackages;

    @Parameter(property = "scanClasses")
    private String scanClasses;

    @Parameter(property = "scanExcludePackages")
    private String scanExcludePackages;

    @Parameter(property = "scanExcludeClasses")
    private String scanExcludeClasses;

    @Parameter(property = "servers")
    private List<String> servers;

    @Parameter(property = "scanDependenciesDisable")
    private Boolean scanDependenciesDisable;

    @Parameter(property = "scanDependenciesJars")
    private List<String> scanDependenciesJars;

    @Parameter(property = "customSchemaRegistryClass")
    private String customSchemaRegistryClass;

    @Parameter(property = "asyncApiVersion")
    private String asyncApiVersion;

    @Parameter(property = "apicurioRegistryUrl")
    private String apicurioRegistryUrl;

    @Parameter(property = "apicurioArtifactId", defaultValue = "${project.artifactId}:${project.version}")
    private String apicurioArtifactId;

    @Parameter(property = "apicurioArtifactType", defaultValue = "ASYNCAPI")
    private String apicurioArtifactType;

    @Parameter(property = "uploadType", defaultValue = "json")
    private String uploadType;

    @Parameter(property = "excludeFromSchemas")
    private List<String> excludeFromSchemas;

    @Parameter(property = "annotationsArtifacts")
    private List<String> annotationsArtifacts;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping schema generation.");
            return;
        }
        try {
            getLog().info(String.format("Generating schema for artifactId = %s, artifactType = %s, version %s. %s",
                    apicurioArtifactId, apicurioArtifactType, projectVersion,
                    (apicurioRegistryUrl != null ? "Schema will be uploaded to " + apicurioRegistryUrl
                            : "Schema will not be uploaded to a registry")));

            final var scanDependencies = scanDependenciesDisable == null || !scanDependenciesDisable;
            IndexCreator indexCreator = new IndexCreator(
                    mavenProject,
                    getLog(),
                    scanDependencies,
                    classesDir,
                    includeDependenciesScopes,
                    includeDependenciesTypes,
                    annotationsArtifacts);

            IndexView index = indexCreator.createIndex();
            AsyncApi26Document schema = generateSchema(index);

            int schemaComponentsSize = schema.getComponents().getSchemas().size();
            int channelsSize = schema.getChannels().getItems().size();

            getLog().info("Generated schema info:\n\t- # of component schemas: " + schemaComponentsSize
                    + "\n\t- # of channels: " + channelsSize);

            if (apicurioRegistryUrl != null) {
                write(schema);
                uploadToApicurio(schema);
            } else {
                write(schema);
            }
        } catch (IOException | ClassNotFoundException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("Could not generate OpenAPI Schema", ex);
        }
    }

    private void uploadToApicurio(AsyncApi26Document schema) throws IOException {
        String schemaOutput = "";
        if (uploadType.equalsIgnoreCase("json")) {
            schemaOutput = AsyncApiSerializer.serialize(schema, Format.JSON);
        } else if (uploadType.equalsIgnoreCase("yaml")) {
            schemaOutput = AsyncApiSerializer.serialize(schema, Format.YAML);
        }

        ApicurioClient client = new ApicurioClient(RegistryClientFactory.create(apicurioRegistryUrl));
        client.upload(apicurioArtifactId, mavenProject.getVersion(), schemaOutput);
    }

    private AsyncApi26Document generateSchema(IndexView index)
            throws IOException, ClassNotFoundException, MojoExecutionException {
        AsyncApiConfig asyncApiConfig = new MavenConfig(getProperties());
        ClassLoader classLoader = getClassLoader(mavenProject);
        IrisAnnotationScanner scanner = new IrisAnnotationScanner(asyncApiConfig, index, classLoader, mavenProject.getName(),
                mavenProject.getGroupId(), mavenProject.getVersion(), IrisObjectMapper.getObjectMapper());
        return scanner.scan();
    }

    private Map<String, String> getProperties() throws IOException {
        // First check if the configProperties is set, if so, load that.
        PropertyMap propertyMap = new PropertyMap();

        if (configProperties != null && configProperties.exists()) {
            Properties p = new Properties();
            try (InputStream is = Files.newInputStream(configProperties.toPath())) {
                p.load(is);
                p.forEach((key, value) -> propertyMap.put((String) key, (String) value));
            }
        }

        // Now add properties set in the maven plugin.
        propertyMap.put(AAIConfig.MODEL_READER, modelReader);
        propertyMap.put(AAIConfig.FILTER, filter);
        propertyMap.put(AAIConfig.SCAN_DISABLE, scanDisabled);
        propertyMap.put(AAIConfig.SCAN_PACKAGES, scanPackages);
        propertyMap.put(AAIConfig.SCAN_CLASSES, scanClasses);
        propertyMap.put(AAIConfig.SCAN_EXCLUDE_PACKAGES, scanExcludePackages);
        propertyMap.put(AAIConfig.SCAN_EXCLUDE_CLASSES, scanExcludeClasses);
        propertyMap.put(AAIConfig.SERVERS, servers);
        propertyMap.put(AsyncApiConstants.SCAN_DEPENDENCIES_DISABLE, scanDependenciesDisable);
        propertyMap.put(AsyncApiConstants.SCAN_DEPENDENCIES_JARS, scanDependenciesJars);
        propertyMap.put(AsyncApiConstants.CUSTOM_SCHEMA_REGISTRY_CLASS, customSchemaRegistryClass);
        if (excludeFromSchemas == null || excludeFromSchemas.isEmpty()) {
            excludeFromSchemas = List.of("com.fasterxml.jackson.databind.JsonNode");
        }
        propertyMap.put(AsyncApiConstants.EXCLUDE_FROM_SCHEMAS, excludeFromSchemas);
        propertyMap.put(AsyncApiConstants.PROJECT_VERSION, projectVersion);

        return propertyMap.getMap();
    }

    private void write(AsyncApi26Document schema) throws MojoExecutionException {
        try {
            if (outputDirectory == null) {
                // no destination file specified => print to stdout
                getLog().info(IrisObjectMapper.getObjectMapper().writeValueAsString(schema));
            } else {
                Path directory = outputDirectory.toPath();
                if (!Files.exists(directory)) {
                    Files.createDirectories(directory);
                }

                writeSchemaFile(directory, schemaFilename + ".yaml", schema, IrisObjectMapper.getYamlObjectMapper());
                writeSchemaFile(directory, schemaFilename + ".json", schema, IrisObjectMapper.getObjectMapper());

                getLog().info("Wrote the schema files to " + outputDirectory.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write the result", e);
        }
    }

    private void writeSchemaFile(Path directory, String filename, AsyncApi26Document schema, ObjectMapper mapper)
            throws IOException {
        Path file = Paths.get(directory.toString(), filename);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), schema);
    }

    private ClassLoader getClassLoader(MavenProject project) throws MojoExecutionException {
        try {
            List<String> compileClasspathElements = project.getCompileClasspathElements();
            compileClasspathElements.add(project.getBuild().getOutputDirectory());
            compileClasspathElements.add(project.getBuild().getTestOutputDirectory());

            URL[] urls = new URL[compileClasspathElements.size()];

            for (int i = 0; i < compileClasspathElements.size(); ++i) {
                urls[i] = new File(compileClasspathElements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (MalformedURLException | DependencyResolutionRequiredException e) {
            getLog().error("Could not get class loader", e);
            throw new MojoExecutionException("Could not generate OpenAPI Schema", e);
        }
    }
}
