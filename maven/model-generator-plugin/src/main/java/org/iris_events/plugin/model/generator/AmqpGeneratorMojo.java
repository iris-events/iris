package org.iris_events.plugin.model.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.*;
import org.iris_events.plugin.model.generator.exception.AmqpGeneratorException;
import org.iris_events.plugin.model.generator.models.ArtifactSource;
import org.iris_events.plugin.model.generator.utils.CustomDependencies;
import org.iris_events.plugin.model.generator.utils.FileInteractor;
import org.iris_events.plugin.model.generator.utils.PathResolver;
import org.iris_events.plugin.model.generator.utils.SchemaFileGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE, requiresProject = false, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AmqpGeneratorMojo extends AbstractMojo {

    @Parameter(property = "artifactSource", required = true)
    ArtifactSource artifactSource;

    @Parameter(property = "apicurioUrl")
    String apicurioUrl;

    @Parameter(property = "asyncApiFilename", defaultValue = "asyncapi.json")
    String asyncApiFilename;

    @Parameter(property = "asyncApiDirectory", defaultValue = "target,generated")
    String asyncApiDirectory;

    // TODO: default to project group id where plugin is executed
    @Parameter(property = "packageName", required = true)
    String packageName;

    @Parameter(property = "modelVersion", required = true)
    String modelVersion;

    @Parameter(property = "modelName", required = true)
    String modelName;

    @SuppressWarnings("unused") //skip is assigned via system property e.g. -Dskip=true
    @Parameter(property = "skip", defaultValue = "false")
    boolean skip;

    /**
     * Define custom dependencies injected into the models pom.xml file. Dependencies should be separated by a comma (,) and in
     * the
     * following format: groupId:artifactId:version
     */
    @Parameter(property = "customDependency", defaultValue = "")
    String customDependency;

    @Component
    PluginDescriptor pluginDescriptor;

    public void execute() throws MojoExecutionException {
        final var log = getLog();

        if (skip) {
            log.info("Skipping model generation as skip flag is set to [true]");
            return;
        }

        final var pathResolver = new PathResolver();
        final var objectMapper = new ObjectMapper();
        final var fileInteractor = new FileInteractor(pathResolver);
        final var customDependencies = new CustomDependencies(customDependency);

        final var schemaFileGenerator = new SchemaFileGenerator(log, pathResolver, fileInteractor,
                objectMapper);
        fileInteractor.cleanUpDirectories(pathResolver.getWorkingDirectory());

        final var generator = new AmqpGenerator(pluginDescriptor.getVersion(), schemaFileGenerator, objectMapper, pathResolver,
                fileInteractor, log,
                packageName, modelVersion, modelName, asyncApiFilename, asyncApiDirectory, apicurioUrl, customDependencies);

        try {
            generator.generate(artifactSource);
        } catch (AmqpGeneratorException e) {
            throw new MojoExecutionException("Exception generating AMQP models", e);
        }
        log.info("Models generated successfully!");
    }
}
