package org.iris_events.plugin.model.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.iris_events.plugin.model.generator.exception.AmqpGeneratorException;
import org.iris_events.plugin.model.generator.models.ArtifactSource;
import org.iris_events.plugin.model.generator.utils.FileInteractor;
import org.iris_events.plugin.model.generator.utils.PathResolver;
import org.iris_events.plugin.model.generator.utils.SchemaFileGenerator;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE, requiresProject = false)
public class AmqpGeneratorMojo extends AbstractMojo {

    @Parameter(property = "artifactSource", required = true)
    ArtifactSource artifactSource;

    @Parameter(property = "apicurioUrl")
    String apicurioUrl;

    @Parameter(property = "asyncApiFilename", defaultValue = "asyncapi.json")
    String asyncApiFilename;

    @Parameter(property = "asyncApiDirectory", defaultValue = "target,generated")
    String asyncApiDirectory;

    @Parameter(property = "packageName", defaultValue = "org.iris_events.iris")
    String packageName;

    @Parameter(property = "modelVersion", required = true)
    String modelVersion;

    @Parameter(property = "modelName", required = true)
    String modelName;

    @SuppressWarnings("unused") //skip is assigned via system property e.g. -Dskip=true
    @Parameter(property = "skip", defaultValue = "false")
    boolean skip;

    public void execute() throws MojoExecutionException {
        Log log = getLog();

        if (skip) {
            log.info("Skipping model generation as skip flag is set to [true]");
            return;
        }

        PathResolver pathResolver = new PathResolver();
        ObjectMapper objectMapper = new ObjectMapper();
        FileInteractor fileInteractor = new FileInteractor(pathResolver);

        SchemaFileGenerator schemaFileGenerator = new SchemaFileGenerator(log, pathResolver, fileInteractor,
                objectMapper);
        fileInteractor.cleanUpDirectories(pathResolver.getWorkingDirectory());

        AmqpGenerator generator = new AmqpGenerator(schemaFileGenerator, objectMapper, pathResolver, fileInteractor, log,
                packageName, modelVersion, modelName, asyncApiFilename, asyncApiDirectory, apicurioUrl);

        try {
            generator.generate(artifactSource);
        } catch (AmqpGeneratorException e) {
            throw new MojoExecutionException("Exception generating AMQP models", e);
        }
        log.info("Models generated successfully!");
    }
}
