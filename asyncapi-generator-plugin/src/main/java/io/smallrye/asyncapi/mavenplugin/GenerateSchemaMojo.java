package io.smallrye.asyncapi.mavenplugin;

import static io.smallrye.asyncapi.mavenplugin.MapManipulator.addToPropertyMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.IndexView;

import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.smallrye.asyncapi.api.AsyncApiConfig;
import io.smallrye.asyncapi.api.AsyncApiConstants;
import io.smallrye.asyncapi.runtime.io.AsyncApiSerializer;
import io.smallrye.asyncapi.runtime.io.Format;
import io.smallrye.asyncapi.runtime.scanner.GidAnnotationScanner;
import io.smallrye.asyncapi.spec.AAIConfig;

@Mojo(name = "generate-schema", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateSchemaMojo extends AbstractMojo {

    /**
     * Directory where to output the schemas.
     * If no path is specified, the schema will be printed to the log.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.directory}/generated/", property = "outputDirectory")
    private File outputDirectory;

    /**
     * Filename of the schema
     * Default to openapi. So the files created will be openapi.yaml and openapi.json.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "asyncapi", property = "schemaFilename")
    private String schemaFilename;

    /**
     * When you include dependencies, we only look at compile and system scopes (by default)
     * You can change that here.
     * Valid options are: compile, provided, runtime, system, test, import
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "compile,system", property = "includeDependenciesScopes")
    private List<String> includeDependenciesScopes;

    /**
     * When you include dependencies, we only look at jars (by default)
     * You can change that here.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "jar", property = "includeDependenciesTypes")
    private List<String> includeDependenciesTypes;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject mavenProject;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "false", property = "skip")
    private boolean skip;

    /**
     * Compiled classes of the project.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDir")
    private File classesDir;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.version}", property = "projectVersion")
    private String projectVersion;

    @SuppressWarnings("unused")
    @Parameter(property = "configProperties")
    private File configProperties;

    // Properies as per AsyncAPI Config.

    @SuppressWarnings("unused")
    @Parameter(property = "modelReader")
    private String modelReader;

    @SuppressWarnings("unused")
    @Parameter(property = "filter")
    private String filter;

    @SuppressWarnings("unused")
    @Parameter(property = "scanDisabled")
    private Boolean scanDisabled;

    @SuppressWarnings("unused")
    @Parameter(property = "scanPackages")
    private String scanPackages;

    @SuppressWarnings("unused")
    @Parameter(property = "scanClasses")
    private String scanClasses;

    @SuppressWarnings("unused")
    @Parameter(property = "scanExcludePackages")
    private String scanExcludePackages;

    @SuppressWarnings("unused")
    @Parameter(property = "scanExcludeClasses")
    private String scanExcludeClasses;

    @SuppressWarnings("unused")
    @Parameter(property = "servers")
    private List<String> servers;

    @SuppressWarnings("unused")
    @Parameter(property = "scanDependenciesDisable")
    private Boolean scanDependenciesDisable;

    @SuppressWarnings("unused")
    @Parameter(property = "scanDependenciesJars")
    private List<String> scanDependenciesJars;

    @SuppressWarnings("unused")
    @Parameter(property = "customSchemaRegistryClass")
    private String customSchemaRegistryClass;

    @SuppressWarnings("unused")
    @Parameter(property = "asyncApiVersion")
    private String asyncApiVersion;

    @SuppressWarnings("unused")
    @Parameter(property = "apicurioRegistryUrl")
    private String apicurioRegistryUrl;

    @SuppressWarnings("unused")
    @Parameter(property = "apicurioArtifactId", defaultValue = "${project.name}:${project.version}")
    private String apicurioArtifactId;

    @SuppressWarnings("unused")
    @Parameter(property = "apicurioArtifactType", defaultValue = "ASYNCAPI")
    private String apicurioArtifactType;

    @SuppressWarnings("unused")
    @Parameter(property = "uploadType", defaultValue = "json")
    private String uploadType;

    @SuppressWarnings("unused")
    @Parameter(property = "excludeFromSchemas")
    private List<String> excludeFromSchemas;

    @Override
    public void execute() throws MojoExecutionException {
        if (!skip) {
            try {
                getLog().info("INFO output of apicurio vars = \nURL = " + apicurioRegistryUrl +
                        "\nArtifactId = " + apicurioArtifactId
                        + "\nArtifactType = " + apicurioArtifactType
                        + "\nProjectVersion = " + projectVersion);

                IndexCreator indexCreator = new IndexCreator(
                        mavenProject,
                        getLog(),
                        scanDependenciesDisable != null && scanDependenciesDisable,
                        classesDir,
                        includeDependenciesScopes,
                        includeDependenciesTypes);

                IndexView index = indexCreator.createIndex();
                AaiDocument schema = generateSchema(index);

                int schemaComponentsSize = schema.components.schemas.size();
                int channelsSize = schema.channels.size();

                getLog().info("Generated schema info:\nComponents: " + schemaComponentsSize + "\nChannels: " + channelsSize);

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
    }

    private void uploadToApicurio(AaiDocument schema) throws IOException, MojoExecutionException {
        String schemaOutput = "";
        String mediaType = "";
        if (uploadType.equalsIgnoreCase("json")) {
            mediaType = "application/json";
            schemaOutput = AsyncApiSerializer.serialize(schema, Format.JSON);
        } else if (uploadType.equalsIgnoreCase("yaml")) {
            mediaType = "application/x-yaml";
            schemaOutput = AsyncApiSerializer.serialize(schema, Format.YAML);
        }
        String artifactName = String.format("%s:%s", apicurioArtifactId, uploadType);
        String contentType = String.format("%s; artifactType=%s", mediaType, apicurioArtifactType);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apicurioRegistryUrl + "/api/artifacts?ifExists=UPDATE");
            post.addHeader("Content-type", contentType);
            post.addHeader("X-Registry-ArtifactId", artifactName);
            post.setHeader("Accept", "application/json");
            post.setEntity(new StringEntity(schemaOutput));

            Boolean execute = client.execute(post, response -> {
                System.out.println(response.toString());
                return response.getStatusLine().getStatusCode() == 200;
            });

            if (!execute) {
                throw new MojoExecutionException(
                        String.format("Could not upload schema definition to Apicurio server (%s)", apicurioRegistryUrl));
            }
        }
    }

    private AaiDocument generateSchema(IndexView index) throws IOException, ClassNotFoundException, MojoExecutionException {
        AsyncApiConfig asyncApiConfig = new MavenConfig(getProperties());
        ClassLoader classLoader = getClassLoader(mavenProject);
        GidAnnotationScanner scanner = new GidAnnotationScanner(asyncApiConfig, index, classLoader);
        return scanner.scan();
    }

    private Map<String, String> getProperties() throws IOException {
        // First check if the configProperties is set, if so, load that.
        PropertyMap propertyMap = new PropertyMap();

        if (configProperties != null && configProperties.exists()) {
            Properties p = new Properties();
            try (InputStream is = Files.newInputStream(configProperties.toPath())) {
                p.load(is);
                propertyMap.putAll((Map) p);
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
        propertyMap.put(AsyncApiConstants.VERSION, asyncApiVersion);
        propertyMap.put(AsyncApiConstants.EXCLUDE_FROM_SCHEMAS, excludeFromSchemas);
        propertyMap.put(AsyncApiConstants.PROJECT_VERSION, projectVersion);

        return propertyMap.getMap();
    }

    private void write(AaiDocument schema) throws MojoExecutionException {
        try {
            String yaml = AsyncApiSerializer.serialize(schema, Format.YAML);
            String json = AsyncApiSerializer.serialize(schema, Format.JSON);
            if (outputDirectory == null) {
                // no destination file specified => print to stdout
                getLog().info(yaml);
            } else {
                Path directory = outputDirectory.toPath();
                if (!Files.exists(directory)) {
                    Files.createDirectories(directory);
                }

                writeSchemaFile(directory, schemaFilename + ".yaml", yaml.getBytes());
                writeSchemaFile(directory, schemaFilename + ".json", json.getBytes());

                getLog().info("Wrote the schema files to " + outputDirectory.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write the result", e);
        }
    }

    private void writeSchemaFile(Path directory, String filename, byte[] contents) throws IOException {
        Path file = Paths.get(directory.toString(), filename);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }

        Files.write(file, contents,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private ClassLoader getClassLoader(MavenProject project) throws MojoExecutionException {
        try {
            List compileClasspathElements = project.getCompileClasspathElements();
            compileClasspathElements.add(project.getBuild().getOutputDirectory());
            compileClasspathElements.add(project.getBuild().getTestOutputDirectory());

            URL[] urls = new URL[compileClasspathElements.size()];

            for (int i = 0; i < compileClasspathElements.size(); ++i) {
                urls[i] = new File((String) compileClasspathElements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (MalformedURLException | DependencyResolutionRequiredException e) {
            getLog().error("Could not get class loader", e);
            throw new MojoExecutionException("Could not generate OpenAPI Schema", e);
        }
    }
}
