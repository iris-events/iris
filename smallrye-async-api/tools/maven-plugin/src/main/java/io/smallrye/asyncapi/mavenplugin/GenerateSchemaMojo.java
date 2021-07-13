package io.smallrye.asyncapi.mavenplugin;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.JarIndexer;
import org.jboss.jandex.Result;

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

    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpath;

    @Parameter(defaultValue = "false", property = "skip")
    private boolean skip;

    /**
     * Compiled classes of the project.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDir")
    private File classesDir;

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

    @Parameter(property = "pathServers")
    private List<String> pathServers;

    @Parameter(property = "operationServers")
    private List<String> operationServers;

    @Parameter(property = "scanDependenciesDisable")
    private Boolean scanDependenciesDisable;

    @Parameter(property = "scanDependenciesJars")
    private List<String> scanDependenciesJars;

    @Parameter(property = "schemaReferencesEnable")
    private Boolean schemaReferencesEnable;

    @Parameter(property = "customSchemaRegistryClass")
    private String customSchemaRegistryClass;

    @Parameter(property = "applicationPathDisable")
    private Boolean applicationPathDisable;

    @Parameter(property = "asyncApiVersion")
    private String asyncApiVersion;

    @Parameter(property = "apicurioRegistryUrl")
    private String apicurioRegistryUrl;

    @Parameter(property = "apicurioArtifactId", defaultValue = "${project.name}:${project.version}")
    private String apicurioArtifactId;

    @Parameter(property = "apicurioArtifactType", defaultValue = "ASYNCAPI")
    private String apicurioArtifactType;

    @Parameter(property = "uploadType", defaultValue = "json")
    private String uploadType;

    @Parameter(property = "convertUnknownToObjectIgnoredPackages", defaultValue = "false")
    private List<String> convertUnknownToObjectIgnoredPackages;

    @Override
    public void execute() throws MojoExecutionException {
        if (!skip) {
            try {
                getLog().info("INFO output of apicurio vars = \nURL = " + apicurioRegistryUrl +
                        "\nArtifactId = " + apicurioArtifactId
                        + "\nArtifactType = " + apicurioArtifactType);

                IndexView index = createIndex();
                AaiDocument schema = generateSchema(index);

                int schemaComponentsSize = schema.components.schemas.size();
                int channelsSize = schema.channels.size();

                getLog().info("Generated schema info:\nComponents: " + schemaComponentsSize + "\nChannels: " + channelsSize);

                if (apicurioRegistryUrl != null) {
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
            mediaType = "applicatino/json";
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

    private IndexView createIndex() throws MojoExecutionException {
        IndexView moduleIndex;
        try {
            moduleIndex = indexModuleClasses();
        } catch (IOException e) {
            throw new MojoExecutionException("Can't compute index", e);
        }
        if (!scanDependenciesDisable()) {
            List<IndexView> indexes = new ArrayList<>();
            indexes.add(moduleIndex);
            for (Object a : mavenProject.getArtifacts()) {
                Artifact artifact = (Artifact) a;
                if (includeDependenciesScopes.contains(artifact.getScope())
                        && includeDependenciesTypes.contains(artifact.getType())) {
                    try {
                        Result result = JarIndexer.createJarIndex(artifact.getFile(), new Indexer(),
                                false, false, false);
                        indexes.add(result.getIndex());
                    } catch (Exception e) {
                        getLog().error("Can't compute index of " + artifact.getFile().getAbsolutePath() + ", skipping", e);
                    }
                }
            }
            return CompositeIndex.create(indexes);
        } else {
            return moduleIndex;
        }
    }

    private boolean scanDependenciesDisable() {
        if (scanDependenciesDisable == null) {
            return false;
        }
        return scanDependenciesDisable;
    }

    // index the classes of this Maven module
    private Index indexModuleClasses() throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> stream = Files.walk(classesDir.toPath())) {

            List<Path> classFiles = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .collect(Collectors.toList());
            for (Path path : classFiles) {
                indexer.index(Files.newInputStream(path));
            }
        }
        return indexer.complete();
    }

    private AaiDocument generateSchema(IndexView index) throws IOException, ClassNotFoundException, MojoExecutionException {
        AsyncApiConfig asyncApiConfig = new MavenConfig(getProperties());

        ClassLoader classLoader = getClassLoader(mavenProject);

        GidAnnotationScanner scanner = new GidAnnotationScanner(asyncApiConfig, index, classLoader);
        return scanner.scan();
    }

    private Map<String, String> getProperties() throws IOException {
        // First check if the configProperties is set, if so, load that.
        Map<String, String> cp = new HashMap<>();
        if (configProperties != null && configProperties.exists()) {
            Properties p = new Properties();
            try (InputStream is = Files.newInputStream(configProperties.toPath())) {
                p.load(is);
                cp.putAll((Map) p);
            }
        }

        // Now add properties set in the maven plugin.
        addToPropertyMap(cp, AAIConfig.MODEL_READER, modelReader);
        addToPropertyMap(cp, AAIConfig.FILTER, filter);
        addToPropertyMap(cp, AAIConfig.SCAN_DISABLE, scanDisabled);
        addToPropertyMap(cp, AAIConfig.SCAN_PACKAGES, scanPackages);
        addToPropertyMap(cp, AAIConfig.SCAN_CLASSES, scanClasses);
        addToPropertyMap(cp, AAIConfig.SCAN_EXCLUDE_PACKAGES, scanExcludePackages);
        addToPropertyMap(cp, AAIConfig.SCAN_EXCLUDE_CLASSES, scanExcludeClasses);
        addToPropertyMap(cp, AAIConfig.SERVERS, servers);
        addToPropertyMap(cp, AsyncApiConstants.SCAN_DEPENDENCIES_DISABLE, scanDependenciesDisable);
        addToPropertyMap(cp, AsyncApiConstants.SCAN_DEPENDENCIES_JARS, scanDependenciesJars);
        addToPropertyMap(cp, AsyncApiConstants.CUSTOM_SCHEMA_REGISTRY_CLASS, customSchemaRegistryClass);
        addToPropertyMap(cp, AsyncApiConstants.VERSION, asyncApiVersion);
        addToPropertyMap(cp, AsyncApiConstants.CONVERT_UNKNOWN_TO_OBJECT_IGNORED_PACKAGES,
                convertUnknownToObjectIgnoredPackages);

        return cp;
    }

    private void addToPropertyMap(Map<String, String> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }

    private void addToPropertyMap(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void addToPropertyMap(Map<String, String> map, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            map.put(key, values.stream().collect(Collectors.joining(",")));
        }
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
